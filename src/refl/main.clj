(ns refl.main
  (:require [clojure.java.io :as io]
            [org.httpkit.client :as client]
            [org.httpkit.sni-client :as sni-client])
  (:gen-class))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))

(defn refl-str [s]
  s)

(defn file [f]
  (io/file f))

(defn -main [& _]
  (let [res (refl-str "foo")]
    (println (.length res)) ;; reflect on string
    (prn (type (into-array [res res]))) ;; make array of unknown type
    (println (.getPath (file ".")))
    (println (:status @(client/get "https://clojure.org"))))) ;; reflect on file

