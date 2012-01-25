(ns dalap.test.html.selector
  (:use clojure.test)
  (:use [clojure.pprint :only (pprint)])

  (:require [dalap.html :as html])
  (:use dalap.html.selector)
  (:use [dalap.html :only [add-class]]))

(def build-dom-node (ns-resolve 'dalap.html 'build-dom-node))
(def compile-selector (ns-resolve 'dalap.html.selector 'compile-selector))
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
           (-> :div.hello compile-selector (match-selector history))))))


(deftest test-match-selector*
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]
        test-match (fn [selector]
                     (match-selector* (map compile-selector selector) history))]

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

(deftest test-match-selector*-custom-types
  (let [item    (CustomType. "hello" "world")
        history [item
                 (build-dom-node :div#custom)
                 (build-dom-node :body)
                 (build-dom-node :html)]
        test-match (fn [selector]
                     (match-selector* (map compile-selector selector) history))]
    (is (= [item history]
           (test-match [:div CustomType])))))


(def bold-class #(html/add-class % "bold"))

(deftest test-defdecorator
  (let [selectors+transformers
        [[:div :p] bold-class
         [CustomType] #(do ["*" (:a %) "*"])
         [:div] #(html/add-class % "happy")

         ;; simple replacements
         [:pre.foo] (fn [el w] (w [1 2 3 4]))
         [:pre.bar] [1 2 3 4]]]
    (doseq [decorator [(gen-decorator selectors+transformers)
                       (gen-decorator
                        (partition 2 selectors+transformers) true)
                       (gen-decorator
                        (reverse (partition 2 selectors+transformers)) true)]]
      (is (= (html/to-html [:div [:p "hello"]]
                           (decorator html/visit)
                           )
             (html/to-html [:div.happy [:p.bold "hello"]])))
      (is (= (html/to-html [:div [:span]] (decorator html/visit))
             (html/to-html [:div.happy [:span]])))
      (is (= (html/to-html [:div] (decorator html/visit))
             (html/to-html [:div.happy])))
      (is (= (html/to-html [:div [:p "hello"]]
                           (decorator html/visit))
             (html/to-html [:div.happy [:p.bold "hello"]])))

      ;; test nested matches
      (is (= (html/to-html [:div [:p "hello" [:div [:p "hello"]]]]
                           (decorator html/visit))
             (html/to-html [:div.happy [:p.bold "hello"
                                        [:div.happy [:p.bold "hello"]]]])))

      (is (= (html/to-html [:div [:p [(CustomType. 1 2)]]]
                           (decorator html/visit))
             (html/to-html [:div.happy [:p.bold "*1*"]])))

      ;; test value replacements
      (is (= (html/to-html [:pre.foo]
                           (decorator html/visit))
             (html/to-html [:pre.bar] (decorator html/visit))
             "1234"
             )))))
