#!/usr/bin/env bb

(ns reflect-config
  (:require [babashka.process :refer [process]]
            [cheshire.core :as cheshire]
            [clojure.string :as str]))

(defn- javaify [classpath cmd]
  (str/join " " (concat ["java" "-cp" classpath] cmd)))

(defn generate-reflect-config [classpath invocations]
  (doseq [trace-cmd invocations]
    (let [config-agent-env "-agentlib:native-image-agent=config-merge-dir=nativecfg"]
      @(process (javaify classpath trace-cmd) {:inherit true :extra-env {"JAVA_TOOL_OPTIONS" config-agent-env}}))))


(defn generate-trace-files [classpath invocations]
  (into []
        (comp (map-indexed vector)
              (map (fn [[n trace-cmd]]
                     (let [trace-file (str "nativecfg/trace-file-" n ".json")
                           trace-agent-env (str "-agentlib:native-image-agent=trace-output=" trace-file)]
                       @(process (javaify classpath trace-cmd) {:inherit true :extra-env {"JAVA_TOOL_OPTIONS" trace-agent-env}})
                       trace-file))))
        invocations))


(defn normalize-array-name [n]
  ({"[F" "float[]"
    "[B" "byte[]"
    "[Z" "boolean[]"
    "[C" "char[]"
    "[D" "double[]"
    "[I" "int[]"
    "[J" "long[]"
    "[S" "short[]"} n n))

;; [Z = boolean
;; [B = byte
;; [S = short
;; [I = int
;; [J = long
;; [F = float
;; [D = double
;; [C = char
;; [L = any non-primitives(Object)

(defn clean-config [config-json trace-json]
  (let [ignored (atom #{})
        unignored (atom #{})

        ignore (fn [{:keys [:tracer :caller_class :function :args] :as _m}]
                 (when (= "reflect" tracer)
                   (when-let [arg (first args)]
                     (let [arg (normalize-array-name arg)]
                       (if (and caller_class
                                (or (= "clojure.lang.RT" caller_class)
                                    (= "clojure.genclass__init" caller_class)
                                    (and (str/starts-with? caller_class "clojure.core$fn")
                                         (= "java.sql.Timestamp" arg)))
                                (= "forName" function))
                         (swap! ignored conj arg)
                         (when (= "clojure.lang.RT" caller_class)
                           ;; unignore other reflective calls in clojure.lang.RT
                           (swap! unignored conj arg)))))))

        _ (run! ignore trace-json)
        ;; _ (prn @ignored)
        ;; _ (prn @unignored)
        process-1 (fn [{:keys [:name] :as m}]
                    (when-not (and (= 1 (count m))
                                   (contains? @ignored name)
                                   (not (contains? @unignored name)))
                      ;; fix bug(?) in automated generated config
                      (if (= "java.lang.reflect.Method" name)
                        (assoc m :name "java.lang.reflect.AccessibleObject")
                        m)))]
    (keep process-1 config-json)))

(defn join-traces [trace-files]
  (into [] (comp (map (fn [f]
                        (cheshire/parse-string (slurp f) true)))
                 cat)
        trace-files))

(defn simplify-reflect-config [reflect-config trace-files]
  (let [reflect-cfg (cheshire/parse-string (slurp reflect-config) true)]
    (clean-config reflect-cfg (join-traces trace-files))))
