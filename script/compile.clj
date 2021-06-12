#!/usr/bin/env bb

(ns compile
  (:require [babashka.process :refer [process sh]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def classpath (str/trim (str/join ":" ["classes" (:out (sh "clojure -Spath"))])))

@(process [(str (io/file (System/getenv "GRAALVM_HOME") "bin" "native-image"))
           "-cp" classpath
           "--initialize-at-build-time"
           "--no-server"
           "--no-fallback"
           "-H:ReflectionConfigurationFiles=reflect-config-cleaned.json"
           "-H:+ReportExceptionStackTraces"
           "refl.main"]
          {:inherit true})
