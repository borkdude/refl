(ns refl.main
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn refl-str [s]
  s)

(defn file [f]
  (io/file f))

(defn -main [& args]
  (let [res (refl-str "foo")]
    (case (first args)
      "string"
      (do (println (.length res))
          (prn (type (into-array [res res])));; make array of unknown type) ;; reflect on string

          )
      "file" (println (.getPath (file ".")))
      (println "Usage: refl.main string | file"))))
