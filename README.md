# refl

A script to clean up reflection configurations produced by the GraalVM
native-image-agent for Clojure projects.

## Problem description

The reflection configs produced by the GraalVM native-image-agent contain many
false positives for Clojure programs. This can be alleviated using a
caller-based filter, like described
[here](https://github.com/lread/clj-graal-docs#reflection). But excluding all
calls from `clojure.lang.RT` may be too coarse for some programs. This repo
offers a finer-grained solution.

## How it works

This project invokes `GRAALVM_HOME/bin/java` on an AOT-ed Clojure program that
does runtime reflection, twice. In both runs the native-image-agent is used. The
first time it is invoked a `trace-file.json` will be produced. The second time a
`reflect-config.json` will be produced. Unfortunately the `reflect-config.json`
isn't very usable for GraalVM native-image yet, since it contains a lot of false
positives. Using the script `script/gen-reflect-config.clj` the information from
both JSON files is combined to create a cleaned up version of the reflect
config, called `reflect-config-cleaned.json`. This config is then used for
native compilation.

## How to use

This project is intended as a reference example on how you _could_ clean up your
generated reflection config. Feel free to copy the code and change it to your
needs. See the [tasks](#tasks) section to see what you can do in this project.

## Requirements

Download GraalVM and set `GRAALVM_HOME`. You will also need `clojure` for
Clojure compilation. Scripts are executed with `bb`
([babashka](https://babashka.org/)).

## The example

This is the example Clojure program that performs reflection at runtime:

``` clojure
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
```

This is the generated [reflection config](reflect-config-cleaned.json), after executing `bb gen-reflect-config`:

``` json
[ {
  "name" : "java.io.File",
  "allPublicMethods" : true
}, {
  "name" : "java.lang.Object[]"
}, {
  "name" : "java.lang.String",
  "allPublicMethods" : true
}, {
  "name" : "java.lang.String[]"
}, {
  "name" : "java.lang.reflect.AccessibleObject",
  "methods" : [ {
    "name" : "canAccess",
    "parameterTypes" : [ "java.lang.Object" ]
  } ]
} ]
```

Note that the raw [`reflect-config.json`](reflect-config.json) is 527 lines long and contains many
false positives, mainly due to calls to `Class/forName` in `clojure.lang.RT` and
some other places. Unfortunately ignoring all calls from `clojure.lang.RT` is
too coarse, since it also does reflection to create arrays.

The `java.lang.reflect.AccessibleObject` is needed because the
`clojure.lang.Reflector` reflectively looks up the `canAccess` method on
`Method` [here](https://github.com/clojure/clojure/blob/b1b88dd25373a86e41310a525a21b497799dbbf2/src/jvm/clojure/lang/Reflector.java#L38). How meta.

## Tasks

See `bb tasks`:

```
The following tasks are available:

compile-clj
classpath
gen-reflect-config
compile-native
```

## License

[Unlicense](https://unlicense.org/).
