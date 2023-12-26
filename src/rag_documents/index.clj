(ns rag-documents.index
  (:require [wkok.openai-clojure.api :as openai-api]
            [rag-documents.text :as txt-split])
  (:import (io.metaloom.qdrant.client.grpc QDrantGRPCClient)
           (io.metaloom.qdrant.client.grpc.proto Collections$CollectionDescription Collections$Distance Collections$ListCollectionsResponse Collections$VectorParams JsonWithInt JsonWithInt$Value Points$Condition Points$FieldCondition Points$Filter Points$GetResponse Points$Match Points$PointId Points$RetrievedPoint Points$ScoredPoint Points$ScrollResponse Points$SearchParams Points$SearchResponse Points$WithPayloadSelector Points$WithVectorsSelector)
           (io.metaloom.qdrant.client.util ModelHelper)
           (java.util HashMap List UUID)))


(def api-key (System/getenv "OPENAI_API_KEY"))
(def collection-name "rag-documents")

(defn generate-payload-selector
  []
  (-> (Points$WithPayloadSelector/newBuilder)
      (.setEnable true)
      (.build)))

(defn generate-vector-selector
  []
  (-> (Points$WithVectorsSelector/newBuilder)
      (.setEnable false)
      (.build)))

(defn vector-params
  [size]
  (-> (Collections$VectorParams/newBuilder)
      (.setSize size)
      (.setDistance Collections$Distance/Cosine)
      (.build)))


(defn qdrant-builder
  [host, port]
  (-> (QDrantGRPCClient/builder)
      (.setHostname host)
      (.setPort port)
      (.build)))

(defn point-from-payload-and-embedding
  [embedding, ^HashMap payload]
  (ModelHelper/point (ModelHelper/pointId (UUID/randomUUID)) (float-array embedding) payload))


(defn get-qdrant-client
  "create a qdrant client instance"
  [host, port]
  (let [^QDrantGRPCClient client (qdrant-builder host port)]
    client))

(def ^QDrantGRPCClient global-qdrant-client (get-qdrant-client "localhost" 6334))

(defn collection-exists?
  [^String collection-name]
  (let [^Collections$ListCollectionsResponse collections (.sync (.listCollections global-qdrant-client))
        collections-list (.getCollectionsList collections)
        names (vec (map #(.getName ^Collections$CollectionDescription %) collections-list))]
    (not (= (some #{collection-name} names) nil))))


(defn- create-collection
  [^String collection-name]
  (-> global-qdrant-client
      (.createCollection collection-name ^Collections$VectorParams (vector-params 1536))
      (.sync)))

(defn create-collection-if-not-exists
  [^String collection-name]
  (if (not (collection-exists? collection-name))
    (create-collection collection-name)
    nil))

(defn int-to-value
  [^Integer value]
  (-> (JsonWithInt$Value/newBuilder)
      (.setIntegerValue value)
      (.build)))

(defn string-to-value
  [^String value]
  (-> (JsonWithInt$Value/newBuilder)
      (.setStringValue value)
      (.build)))

(defn create-payload
  "create payload for qdrant using a single chunk and the filedata"
  [{idx :idx text :chunk}, ^String filename, ^String filepath]
  {"idx" (int-to-value idx), "text" (string-to-value (txt-split/escape-newlines text)), "filename" (string-to-value filename), "filepath" (string-to-value filepath)})

(defn create-embedding-sync
  "use openai api to create embedding for text part"
  [^String text]
  (float-array (get-in (openai-api/create-embedding {:model "text-embedding-ada-002" :input text} {:api-key api-key}) [:data 0 :embedding])))


(defn create-embedding-multi-sync
  "use openai api to create embedding for text part"
  [text]
  (into [] (map #(float-array (get % :embedding))
                (get (openai-api/create-embedding {:model "text-embedding-ada-002" :input text} {:api-key api-key}) :data))))

(defn insert-chunk
  [chunk, collection-name, filename, filepath]
  (let [payload (create-payload chunk filename filepath)
        embedding (create-embedding-sync (get chunk :chunk))
        point (point-from-payload-and-embedding embedding payload)]
    (.sync (.upsertPoint global-qdrant-client collection-name point false))
    nil))

(defn chunks-to-text
  [chunks]
  (->> chunks
       (map :chunk)
       (into)))


(defn merge-chunks-embeddings
  [chunks, embeddings]
  (into [] (map #(do {:chunk %1, :embedding %2}) chunks embeddings)))

(defn chunks-to-points
  [chunks, filename, filepath]
  (let [texts (chunks-to-text chunks)
        embeddings (create-embedding-multi-sync texts)
        merged (merge-chunks-embeddings chunks embeddings)]
    (loop [points []
           merged merged]
      (if (not (empty? merged))
        (recur (conj points
                     (point-from-payload-and-embedding (get (first merged) :embedding)
                                                       (create-payload (get (first merged) :chunk) filename filepath)))
               (rest merged))
        points))))

(defn insert-chunks
  [chunks, collection-name, filename, filepath]
  (let [points (chunks-to-points chunks filename filepath)]
    (if (not (empty? points))
      (-> global-qdrant-client
          (.upsertPoints collection-name points true)
          (.sync))
      nil)))

(defn condition-for-filepath
  [filepath]
  (-> (Points$Condition/newBuilder)
      (.setField (-> (Points$FieldCondition/newBuilder)
                     (.setKey "filepath")
                     (.setMatch (-> (Points$Match/newBuilder)
                                    (.setKeyword filepath)
                                    (.build)))
                     (.build)))
      (.build)))

(defn- retrieved-point-to-map
  [^Points$RetrievedPoint point]
  (let [payload (.getPayloadMap point)]
    {:id       (.getUuid (.getId point))
     :idx      (.getIntegerValue ^JsonWithInt$Value (get payload "idx"))
     :filename (.getStringValue ^JsonWithInt$Value (get payload "filename"))
     :filepath (.getStringValue ^JsonWithInt$Value (get payload "filepath"))
     :text     (.getStringValue ^JsonWithInt$Value (get payload "text"))}))


(defn- query-qdrant-for-points
  [points, collection-name]
  (.getResultList ^Points$GetResponse (.sync (.getPoints global-qdrant-client collection-name
                                                         ^Points$WithPayloadSelector (generate-payload-selector)
                                                         ^Points$WithVectorsSelector (generate-vector-selector)
                                                         ^List (into (map #(ModelHelper/pointId (UUID/fromString (key %))) points))))))

(defn- unique-filepath
  [mapped-points]
  (loop [result {}
         xs mapped-points]
    (if (empty? xs)
      result
      (let [first-point (first xs)
            filepath (:filepath first-point)]
        (recur (assoc result filepath (conj (get result filepath []) first-point))
               (rest xs))))))

(defn- merge-payloads-with-score
  [points, mapped-points]
  (map #(merge % (get points (get % :id))) mapped-points))

(defn query-payloads-for-points
  [points, collection-name]
  (->> (query-qdrant-for-points points collection-name)
       (map #(retrieved-point-to-map %))
       (merge-payloads-with-score points)
       (sort #(> (get %1 :score) (get %2 :score)))))

(defn query-by-vector
  [vector, collection-name, limit, threshold]
  (apply merge (map #(assoc {} (.getUuid (.getId ^Points$ScoredPoint %)) {:id    (.getUuid (.getId ^Points$ScoredPoint %))
                                                                          :score (.getScore %)})
                    (.getResultList ^Points$SearchResponse (.sync (.searchPoints global-qdrant-client collection-name nil vector (int limit) (float threshold)))))))

(defn generate-filter-for-filepath
  [filepath]
  (-> (Points$Filter/newBuilder)
      (.addMust ^Points$Condition (condition-for-filepath filepath))
      (.build)))


(defn- scroll-result-to-vec
  [^Points$ScrollResponse scroll-result]
  (let [result-list (.getResultList scroll-result)]
    (vec (map #(.getId ^Points$RetrievedPoint %) result-list))))

(defn search-points-by-filepath
  [filepath, collection-name]
  (loop [result []
         ^Points$PointId offset nil]
    (if (or (nil? offset) (.hasUuid offset))
      (let [^Points$ScrollResponse scroll-result (.sync (.scrollPoint global-qdrant-client collection-name offset (int 10) (generate-filter-for-filepath filepath) (generate-payload-selector) (generate-vector-selector)))
            vec-result (scroll-result-to-vec scroll-result)]
        (if (not (empty? vec-result))
          (recur (into result vec-result)
                 (.getNextPageOffset scroll-result))
          result))
      result)))


(defn delete-for-filepath
  [filepath, collection-name]
  (let [relevant-points (search-points-by-filepath filepath collection-name)]
    (if (not (empty? relevant-points))
      (-> global-qdrant-client
          (.deletePoints collection-name true (into-array relevant-points))
          (.sync))
      nil)))


(defn query
  [query-str]
  (let [query-embedding (create-embedding-sync query-str)
        queried-points (query-by-vector query-embedding collection-name 100 0.7)]
    ;; TODO: implement TF-IDF on top of this ranking, to get better results
    (query-payloads-for-points queried-points collection-name)))


(defn index-text
  "index text to the corresponding filename"
  [^String text, filepath, filename]
  (delete-for-filepath filepath collection-name)            ; NOTE: this has to be done, in order to avoid duplicates
  (let [chunks (txt-split/split-text-chunked text 700)]
    (insert-chunks chunks collection-name filename filepath)))
