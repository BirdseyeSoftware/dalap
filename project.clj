(defproject dalap "0.0.1-SNAPSHOT"
  :description "Extremely flexible serialization / template library for Clojure.
The name is the acronym for Decide As Late As Possible, from lean
programming."

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.match "0.2.0-alpha9"]]

  :repositories {"snapshots" {:url "s3p://lein-snapshots/snapshots"}})
