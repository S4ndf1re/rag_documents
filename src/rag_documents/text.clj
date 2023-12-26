(ns rag-documents.text
  (:require [clojure.string :as str]
            [pantomime.extract :as extract]))


(defn preprocess-text
  [text]
  (-> text
      (str/replace #" +" " ")
      (str/replace "\r\n" "\n")
      (str/replace #"\n" " ")
      (str/replace #"[\.\?!]\s+" "\n")
      (str/replace #"^[ |\t]+" "")))

(defn extract-text
  "Extract text and mime-type from any document"
  [^bytes document-path]
  (preprocess-text (:text (extract/parse document-path))))


(defn escape-newlines
  [text]
  (-> text
      (str/replace #"\n" "\\\\n")))

(defn- get-seperator-list
  [] '("\n\n" "\n" " " ""))

(defn- combine-texts
  [text1, text2, separator]
  (if (empty? text1) text2
                     (str text1 separator text2)))

(defn- merge-chunks
  [chunks, chunk-size, separator]
  (loop [final-chunks []
         merged-chunk ""
         xs chunks]
    (if (> (count (first xs)) chunk-size)
      (do
        (println "Warning: added chunk is bigger than chunk-size")
        (recur (conj final-chunks (first xs)) "" (rest xs)))
      (if (empty? xs) (conj final-chunks merged-chunk)
                      (if (<= (+ (count (combine-texts merged-chunk (first xs) separator))) chunk-size)
                        (recur final-chunks (combine-texts merged-chunk (first xs) separator) (rest xs))
                        (recur (conj final-chunks merged-chunk) "" xs))))))



(defn- split-text-priv
  [text, chunk-size, separator-list]
  (let [splitted-chunks (str/split text (re-pattern (first separator-list)))]
    (loop [chunks []
           xs splitted-chunks]
      (if (empty? xs) (merge-chunks chunks chunk-size (first separator-list))
                      (if (<= (count (first xs)) chunk-size)
                        (recur (conj chunks (first xs)) (rest xs))
                        (recur (into
                                 chunks
                                 (split-text-priv (first xs) chunk-size (rest separator-list)))
                               (rest xs)))))))


(defn enumerate-chunks
  [chunks]
  (map-indexed (fn [idx chunk] {:idx idx :chunk chunk}) chunks))

(defn split-text-chunked
  "Split text into chunks using Recursive Character Text Splitting"
  [text, chunk-size]
  (let [separator-list (get-seperator-list)]
    (-> text
        (preprocess-text)
        (split-text-priv chunk-size separator-list)
        (enumerate-chunks))))




