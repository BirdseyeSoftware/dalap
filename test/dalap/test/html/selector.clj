(ns dalap.test.html.selector
  (:use clojure.test)
  (:use [clojure.pprint :only (pprint)])

  (:require [dalap.html :as html])
  (:use dalap.html.selector)
  (:use [dalap.html :only [add-class]]))

(def build-dom-node
  (ns-resolve 'dalap.html
              'build-dom-node))

(deftest test-dom-matches-selector
  (is (dom-matches-selector? (build-dom-node :p#uno.hello)
                             :p#uno.hello))
  (is (not (dom-matches-selector? (build-dom-node :p)
                                  :p.hello))))

(deftest test-match-selector
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]]
  (is (= [nil history]
         (match-selector :p.hello history)))
  (is (= [(->> history (drop 1) first) (take 1 history)]
         (match-selector :div.hello history)))))


(deftest test-match-selector*
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]]

    (is (= [(build-dom-node :p) history]
           (match-selector* [:p] history)))

    (is (= [(build-dom-node :p) history]
           (match-selector* [:div.hello :p] history)))

    (is (= [nil history]
           (match-selector* [:div.hello :p.world] history)))

    ;; :div.hello is not in the start of the history
    ;; so it doesn't match
    (is (= [nil history]
           (match-selector* [:div.hello] history)))

    (is (= [nil history]
           (match-selector* [:body :div.not-there] history)))))

(defrecord CustomType [a b])

(deftest test-match-selector*-custom-types
  (let [item    (CustomType. "hello" "world")
        history [item
                 (build-dom-node :div#custom)
                 (build-dom-node :body)
                 (build-dom-node :html)]]
  (is (= [item history]
         (match-selector* [:div `CustomType] history)))))


(def bold-class #(html/add-class % "bold"))

(deftest test-defdecorator
  (let [selectors+actions [[:div :p] bold-class
                           [:div] #(html/add-class % "happy")]]
    (doseq [decorator [(gen-decorator selectors+actions)
                       (gen-decorator (partition 2 selectors+actions)
                                      true)
                       (gen-decorator (reverse
                                       (partition 2 selectors+actions))
                                      true)]]
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
             (html/to-html [:div.happy [:p.bold "hello"]]))))))
