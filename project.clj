(defproject com.birdseye-sw/dalap "0.1.0"
  :description "Extremely flexible serialization / template library for Clojure.
The name is the acronym for Decide As Late As Possible, from lean
programming."
  :url "http://birdseye-sw.com/oss/dalap/"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[com.birdseye-sw/buster-cljs "0.1.0"]]}}
  ;;:plugins [[lein-cljsbuild "0.2.9"]
  ;;          [com.birdseye-sw/lein-dalap "0.1.0"]]
  ;;:hooks [leiningen.dalap]

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
  )
