#!/usr/bin/env bb

(ns compile
  (:require [babashka.process :refer [process]]
            [clojure.java.io :as io]))

(defn compile-native-image [classpath]
  @(process [(str (io/file (System/getenv "GRAALVM_HOME") "bin" "native-image"))
             "-cp" classpath
             "--initialize-at-build-time"
             "--no-server"
             "--no-fallback"
             "-H:ReflectionConfigurationFiles=nativecfg/reflect-config-cleaned.json"
             "-H:+ReportExceptionStackTraces"
             "refl.main"]
            {:inherit true}))
