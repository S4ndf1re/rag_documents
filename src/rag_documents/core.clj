(ns rag-documents.core
  (:require
    [clojure.java.io :as io]
    [pantomime.mime :as mime]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [rag-documents.index :as index]
    [rag-documents.files :as files]
    [rag-documents.util :as util]
    [rag-documents.text :as text]
    [clojure.data.json :as json])


  (:import
    (java.nio.charset StandardCharsets)))



(def ready-to-index (atom {}))

(defn scan-and-delete-ready-to-index
  "Scan ready-to-index atom and delete all documents that are older than 5 minutes"
  []
  (let [current-time (System/currentTimeMillis)
        ready-to-index-map @ready-to-index]
    (doseq [[document-path document-data] ready-to-index-map]
      (if (> (- current-time (:request-time document-data)) 300000)
        (do
          (io/delete-file (io/file document-path))
          (swap! ready-to-index dissoc document-path))))))

(defn upload-document
  "upload a document into the local file system"
  [request]
  (let [document-data (util/input-stream-to-bytes (:body request))
        document-text (text/extract-text document-data)
        should_overwrite (Boolean/valueOf ^String (get-in request [:query-params :overwrite] false))
        document-mime-type (mime/mime-type-of document-data)
        document-name (get-in request [:query-params :name] (str (System/currentTimeMillis) "" (mime/extension-for-name document-mime-type)))
        document-path (str "./documents/" document-name)
        estm_pricing (util/estimate-embedding-cost document-text)]
    (try
      (scan-and-delete-ready-to-index)
      (if (not (files/is-mime-type-accepted? document-mime-type))
        (throw (Exception. (str "Mime-Type " document-mime-type " not accepted"))))
      (files/save-file document-data document-path @ready-to-index :should-overwrite should_overwrite)
      (swap! ready-to-index assoc document-path {:name document-name :estm_pricing estm_pricing :request-time (System/currentTimeMillis)})
      {:status  201
       :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
       :body    (json/write-str {:name         document-name
                                 :path         document-path
                                 :text         document-text
                                 :mime_type    document-mime-type
                                 :estm_pricing estm_pricing})}
      (catch Exception e
        {:status  400
         :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
         :body    (json/write-str {
                                   :message (ex-message e)})}))))

(defn index-document
  "Index the document provided by api"
  [request]
  (let [request-data (json/read-str (String. ^bytes (util/input-stream-to-bytes (:body request)) StandardCharsets/UTF_8))
        document-path (get request-data "path")
        document-text (get request-data "text")
        document-name (get request-data "name")]
    (scan-and-delete-ready-to-index)
    (try
      (if (not (contains? @ready-to-index document-path))
        (throw (Exception. (str "Document " document-path " not ready to index, because it wasn't upload in time. Try again by re-uploading the document with overwrite enabled."))))
      (if (and (files/does-file-exist? document-path) (files/path-valid? document-path))
        (do (index/index-text document-text document-path document-name)
            (swap! ready-to-index dissoc document-path)
            {:status  201
             :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
             :body    (json/write-str {:message (str "Document " document-path " indexed successfully")})}))
      (catch Exception e
        {:status  400
         :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
         :body    (json/write-str {:message (ex-message e)})}))))


(defn cancel-upload
  "Cancel upload of document"
  [request]
  (let [body-json (json/read-str (String. ^bytes (util/input-stream-to-bytes (:body request)) StandardCharsets/UTF_8))
        document-path (get body-json "path")]
    (try
      (if (not (contains? @ready-to-index document-path))
        (throw (Exception. (str "Document " document-path " is already canceled, possibly due to timeout of 5 minutes."))))
      (if (and (files/path-valid? document-path) (files/does-file-exist? document-path))
        (io/delete-file (io/file document-path)))
      (swap! ready-to-index dissoc document-path)
      {:status  201
       :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
       :body    (json/write-str {:message (str "Document " document-path " canceled successfully")})}
      (catch Exception e
        {:status  400
         :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
         :body    (json/write-str {:message (ex-message e)})}))))

(defn load-files
  [request]
  (let [existing-files (files/load-existing-files "./documents/" @ready-to-index)]
    (scan-and-delete-ready-to-index)
    {:status  200
     :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
     :body    (json/write-str existing-files)}))


(defn load-file
  [request]
  (let [file-name (get-in request [:query-params :name])
        file-path (str "./documents/" file-name)]
    (scan-and-delete-ready-to-index)
    (if (and (files/path-valid? file-path) (files/does-file-exist? file-path))
      {:status  200
       :headers {"Content-Type" "application/octet-stream", "Access-Control-Allow-Origin" "*"}
       :body    (json/write-str {:title     file-name
                                 :mime_type (mime/mime-type-of (io/file file-path))
                                 :text      (text/extract-text (util/read-file-to-bytes (io/file file-path)))})}
      {:status  404
       :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
       :body    (json/write-str {:message (str "File " file-name " not found")})})))

(defn delete-file
  [request]
  (let [file-name (get-in request [:query-params :name])
        file-path (str "./documents/" file-name)]
    (if (and (files/path-valid? file-path) (files/does-file-exist? file-path))
      (try
        (io/delete-file (io/file file-path))
        (index/delete-for-filepath file-path index/collection-name)
        {:status  200
         :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
         :body    (json/write-str {:message (str "File " file-name " deleted successfully")})}
        (catch Exception e
          {:status  500
           :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
           :body    (json/write-str {:message (ex-message e)})}))
      {:status  404
       :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
       :body    (json/write-str {:message (str "File " file-name " not found")})})))

(defn delete-file-ok
  [request]
  {:status  200
   :headers {"Access-Control-Allow-Origin" "*", "Access-Control-Allow-Methods" "DELETE"}})

(defn query-files
  [request]
  (let [query-str (get-in request [:query-params :query])
        query-result (index/query query-str)]
    {:status  200
     :headers {"Content-Type" "application/json", "Access-Control-Allow-Origin" "*"}
     :body    (json/write-str query-result)}))

(def routes
  (route/expand-routes
    #{["/api/upload" :post upload-document :route-name :api-upload]
      ["/api/index" :post index-document :route-name :api-index]
      ["/api/load" :get load-files :route-name :api-load]
      ["/api/load-file" :get load-file :route-name :api-load-file]
      ["/api/delete-file" :delete delete-file :route-name :api-delete-file]
      ["/api/delete-file" :options delete-file-ok :route-name :api-delete-file-ok]
      ["/api/query" :get query-files :route-name :api-query]
      ["/api/cancel" :post cancel-upload :route-name :api-cancel]}))




(defn create-server []
  (http/create-server {::http/routes routes
                       ::http/type   :jetty
                       ::http/port   8080
                       ::http/join?  false}))

(defn start-server
  []
  (index/create-collection-if-not-exists index/collection-name)
  (http/start (create-server)))

(defn stop-server
  [server]
  (http/stop server))

(defn -main
  [& args]
  (index/create-collection-if-not-exists index/collection-name)
  (http/start (create-server)))




