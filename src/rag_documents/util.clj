(ns rag-documents.util
  (:require [clojure.java.io :as io])
  (:import (com.knuddels.jtokkit Encodings)
           (com.knuddels.jtokkit.api ModelType)
           (java.io ByteArrayOutputStream File InputStream)))

(defn inspect
  [x]
  (println (str x))
  x)

(defn input-stream-to-bytes
  "Convert input stream to bytes"
  [^InputStream input-stream]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy input-stream out)
    (.toByteArray out)))

(defn read-file-to-bytes
  "Read file to bytes"
  [^File file]
  (with-open [in (io/input-stream file)]
    (input-stream-to-bytes in)))


(defn count_tokens_for_text
  "Count tokens for text"
  [^String text]
  (let [registry (Encodings/newDefaultEncodingRegistry)
        enc (.getEncodingForModel registry ModelType/TEXT_EMBEDDING_ADA_002)]
    (count (.encode enc text))))

(defn estimate-embedding-cost
  "Estimate embedding cost for OpenAI ADA-002 embedding API"
  [^String text]
  (let [token_count (count_tokens_for_text text)]
    (* (/ token_count 1000) 0.0001)))
