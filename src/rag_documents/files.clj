(ns rag-documents.files
  (:require [clojure.java.io :as io]
            [pantomime.mime :as mime])
  (:import (java.io File FileOutputStream)))

(def accepted-filetypes #{"application/pdf"
                          "text/plain"
                          "application/x-tex"})

(defn is-mime-type-accepted?
  "Check if mime-type is accepted"
  [mime-type]
  (contains? accepted-filetypes mime-type))

(defn write-and-close-stream
  [stream, data]
  (.write stream data)
  (.close stream))

(defn does-file-exist?
  "check if file exist"
  [^String path]
  (.exists (File. path)))

(defn path-valid?
  "check if path is valid"
  [^String path]
  (and (.startsWith path "./documents/") (not (.contains path ".."))))

(defn save-file
  "Save file to disk"
  [^bytes document-data, path, ready-to-index & options]
  (let [file-exists (does-file-exist? path)
        {should-overwrite :should-overwrite} options
        file (io/file path)
        stream (FileOutputStream. file false)]

    (if (or should-overwrite (ready-to-index path)) (write-and-close-stream stream document-data)
                                                    (if file-exists (throw (Exception. (str "File " path " already exist")))
                                                                    (write-and-close-stream stream document-data)))))

(defn load-existing-files
  "Load existing files from disk together with last_modified timestamp
  (since 1. Jan. 1970 in Milliseconds)"
  [path, ready-to-index]
  (let [files (->> (io/file path)
                   (.listFiles)
                   (filter #(.isFile %))
                   (filter #(not (contains? ready-to-index (.getPath %))))
                   (map #(assoc {} (.getPath %) {:name (.getName %) :last_modified (.lastModified %) :mime_type (mime/mime-type-of (io/file %))}))
                   (apply merge))]
    (if (not files) {}
                    files)))
