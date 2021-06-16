(ns refl.main
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn refl-str [s]
  s)

(defn file [f]
  (io/file f))

(defn -main [arg]
  (let [res (refl-str "foo")]
    (if (= arg "string")
      (do (println (.length res))
          (prn (type (into-array [res res])));; make array of unknown type) ;; reflect on string

          )
      ;; reflect on file
      (println (.getPath (file "."))))))
