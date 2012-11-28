(defproject dalap "0.0.1-SNAPSHOT"
  :description "Extremely flexible serialization / template library for Clojure.
The name is the acronym for Decide As Late As Possible, from lean
programming."

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [buster-cljs "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "0.2.9"]
            [lein-dalap "0.1.0-SNAPSHOT"]]

  :hooks [leiningen.dalap]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.1"]]}}

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-path "src/cljs"
     :compiler
     {:output-to "resources/js/dalap_core_dev.js"
      :optimizations :whitespace
      :pretty-print true}}
    ;;;
    {:id "browser-test"
     :source-path "test/cljs"
     :notify-command ["buster_runner.sh"]
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"]
      :output-to "resources/js/dalap_core_browser_test.js"
      :optimizations :simple
      :pretty-print true}}
    ;;;
    {:id "node-test"
     :source-path "test/cljs"
     :notify-command ["buster_runner.sh"]
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"]
      :output-to "resources/js/dalap_core_node_test.js"
      :optimizations :simple
      :pretty-print true}}
    ;;;
    {:id "browser-test-optimized"
     :source-path "test/cljs"
     :notify-command ["buster_runner.sh"]
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"]
      :output-to "resources/js/dalap_core_browser_test_optimized.js"
      :optimizations :advanced}}
    ]}
  :repositories {"snapshots" {:url "s3p://lein-snapshots/snapshots"}})
