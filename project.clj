(defproject rag_documents "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.novemberain/pantomime "2.11.0"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.route "0.6.0"]
                 [org.slf4j/slf4j-simple "2.0.9"]
                 [org.clojure/data.json "2.4.0"]
                 [io.metaloom.qdrant/qdrant-java-grpc-client "0.13.0"]
                 [net.clojars.wkok/openai-clojure "0.14.0"]
                 [com.knuddels/jtokkit "0.6.1"]]




  :repl-options {:init-ns rag-documents.core})
