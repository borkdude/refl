(ns refl.main
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn refl-str [s]
  s)

(defn file [f]
  (io/file f))

(defn -main [& _]
  (let [res (refl-str "foo")]
    (println (.length res)) ;; reflect on string
    (prn (type (into-array [res res]))) ;; make array of unknown type
    (println (.getPath (file "."))))) ;; reflect on file

