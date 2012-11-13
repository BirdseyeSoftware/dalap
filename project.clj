(defproject dalap "0.0.1-SNAPSHOT"
  :description "Extremely flexible serialization / template library for Clojure.
The name is the acronym for Decide As Late As Possible, from lean
programming."

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.match "0.2.0-alpha9"]
                 [buster-cljs "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "0.2.9"]
            [lein-dalap-cljsbuild "0.1.0-SNAPSHOT"]]
  :hooks [leiningen.dalap-cljsbuild]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]

  :cljsbuild
  {:builds
   [{:id :dev
     :source-path "src/cljs"
     :dalap
     {:paths
      {"src/clj/dalap/walk.clj" "src/cljs/dalap/walk.cljs"
       "src/clj/dalap/selector.clj" "src/cljs/dalap/selector.cljs"}}
     :compiler
     {:output-to "resources/js/dalap_core_dev.js"
      :optimizations :whitespace
      :pretty-print true}}
    ;;;
    {:id :browser-test
     :source-path "test/cljs"
     :dalap
     {:paths
      {"test/clj/dalap/test/selector_test.clj" "test/cljs/dalap/test/selector_test.cljs"
       "test/clj/dalap/test/walk_test.clj" "test/cljs/dalap/test/walk_test.cljs" }}
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"] ;;["src/cljs"]
      :output-to "resources/js/dalap_core_browser_test.js"
      :optimizations :simple
      :pretty-print true}}
    ;;;
    {:id :node-test
     :source-path "test/cljs"
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"] ;;["src/cljs"]
      :output-to "resources/js/dalap_core_node_test.js"
      :optimizations :simple
      :pretty-print true}}
    ;;;
    {:id :browser-test-optimized
     :source-path "test/cljs"
     ;; :dalap
     ;; {:paths
     ;;  {"test/clj/dalap/test/selector_test.clj" "test/cljs/dalap/test/selector_test.cljs"
     ;;   "test/clj/dalap/test/walk_test.clj" "test/cljs/dalap/test/walk_test.cljs"}}
     :compiler
     {:externs ["externs/buster.js"]
      :libraries ["resources/js/dalap_core_dev.js"] ;; ["src/cljs"]
      :output-to "resources/js/dalap_core_browser_test_optimized.js"
      :optimizations :advanced}}
    ]}
  :repositories {"snapshots" {:url "s3p://lein-snapshots/snapshots"}})
