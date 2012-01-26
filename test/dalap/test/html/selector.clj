(ns dalap.test.html.selector
  (:use clojure.test)
  (:use [clojure.pprint :only (pprint)])

  (:require [dalap.html :as html])
  (:use dalap.html.selector)
  (:use [dalap.html :only [add-class]]))

(def build-dom-node (ns-resolve 'dalap.html 'build-dom-node))

(def dom-matches-tag-selector?
  (ns-resolve 'dalap.html.selector 'dom-matches-tag-selector?))

(deftest test-dom-matches-selector
  (is (dom-matches-tag-selector? (build-dom-node :p#uno.hello)
                                 :p#uno.hello))
  (is (not (dom-matches-tag-selector? (build-dom-node :p)
                                      :p.hello))))

(deftest test-match-selector
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]]
    (is (= [nil history]
           (match-selector :p.hello history)))
    (is (= [(->> history (drop 1) first) (take 1 history)]
           (-> :div.hello to-node-matcher (match-selector history))))))


(deftest test-match-selector*
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]
        test-match (fn [selector]
                     (match-selector* (map to-node-matcher selector) history))]

    (is (= [(build-dom-node :p) history]
           (test-match [:p])))

    (is (= [(build-dom-node :p) history]
           (test-match [:div.hello :p])))

    (is (= [nil history]
           (test-match [:div.hello :p.world])))

    ;; :div.hello is not in the start of the history
    ;; so it doesn't match
    (is (= [nil history]
           (test-match [:div.hello])))

    (is (= [nil history]
           (test-match [:body :div.not-there])))))

(defrecord CustomType [a b])
(defrecord CustomType2 [a b])
(defrecord CustomType3 [a b])
(deftype CustomType4 [a b])

(deftest test-match-selector*-custom-types
  (let [item    (CustomType. "hello" "world")
        history [item
                 (build-dom-node :div#custom)
                 (build-dom-node :body)
                 (build-dom-node :html)]
        test-match (fn [selector]
                     (match-selector* (map to-node-matcher selector) history))]
    (is (= [item history]
           (test-match [:div CustomType])))))


(def bold-class #(html/add-class % "bold"))

(deftest test-decorators
  (let [selectors+transformers
        [[:div :p] bold-class
         [:div] #(html/add-class % "happy")

         [CustomType] :a
         [CustomType2] (fn [o w] ["*" (:a o) "*"])
         ;; the following anon function doesn't get wrapped in 32 bit
         ;; / java 1.6 jvms for some reason TODO
         ;;[#(= CustomType3 (type %))] #(do ["*" (:a %) "*"])
         [#(= CustomType3 (type %))] (fn [o w] ["*" (:a o) "*"])
         [CustomType4] (fn [o w] ["*" (.a o) "*"])

         ;; simple replacements
         [:pre.foo] (fn anon-vis [el w] (w [1 2 3 4]))
         [:pre.bar] [1 2 3 4]

         ;; using sets/maps as predicates and visitors
         [:span.hundred #{89 88}] {88 198, 89 199}
         [:p #{89 88}] {88 98, 89 99}
         ]]
    (doseq [decorator [(gen-decorator selectors+transformers)
                       (gen-decorator
                        (partition 2 selectors+transformers) true)
                       (gen-decorator
                        (reverse (partition 2 selectors+transformers))
                        true)]]
      (let [visitor (decorator html/visit)
            vis     #(html/to-html % visitor)]
        (is (= (vis [:div [:p "hello"]])
               (html/to-html [:div.happy [:p.bold "hello"]])))
        (is (= (vis [:div [:span]])
               (html/to-html [:div.happy [:span]])))
        (is (= (vis [:div])
               (html/to-html [:div.happy])))
        (is (= (vis [:div [:p "hello"]])
               (html/to-html [:div.happy [:p.bold "hello"]])))

        ;; test nested matches
        (is (= (vis [:div [:p "hello" [:div [:p "hello"]]]])
               (html/to-html [:div.happy [:p.bold "hello"
                                          [:div.happy [:p.bold "hello"]]]])))

        ;; custom records/types
        (is (= (vis [:div [:p [(CustomType. 999 888)]]])
               (html/to-html [:div.happy [:p.bold "999"]])))
        (is (= (vis [:div [:p [(CustomType2. "--" "^^")]]])
               (html/to-html [:div.happy [:p.bold "*--*"]])))
        (is (= (vis [:div [:p [(CustomType3. (range 10) "foo")]]])
               (html/to-html [:div.happy [:p.bold "*0123456789*"]])))
        (is (= (vis [:div [:p [(CustomType4. [:b "a"] "b")]]])
               (html/to-html [:div.happy [:p.bold ["*" [:b "a"] "*"]]])))

        ;; using sets/maps
        (is (= (vis [:div [:p [88 89]]])
               (html/to-html [:div.happy [:p.bold "9899"]])))
        (is (= (vis [:div [:span.hundred [88 89]]])
               (html/to-html [:div.happy [:span.hundred "198199"]])))

        ;; test value replacements
        (is (= (vis [:pre.foo])
               (vis [:pre.bar])
               "1234"))))))
