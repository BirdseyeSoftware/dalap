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

(deftest test-track-selector
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]]
  (is (= [nil history]
         (track-selector :p.hello history)))
  (is (= [(->> history (drop 1) first) (take 1 history)]
         (track-selector :div.hello history)))))


(deftest test-select-element
  (let [history [(build-dom-node :p)
                 (build-dom-node :div.hello)
                 (build-dom-node :body)
                 (build-dom-node :html)]]

  (is (= [(build-dom-node :p) history]
         (select-element [:div.hello :p] history)))

  (is (= [nil history]
         (select-element [:div.hello :p.world] history)))

  ; :div.hello is not in the start of the history
  ; so it doesn't match
  (is (= [nil history]
         (select-element [:div.hello] history)))

  (is (= [nil history]
         (select-element [:body :div.not-there] history)))))

(defrecord CustomType [a b])

(deftest test-select-element-custom-types
  (let [item    (CustomType. "hello" "world")
        history [item
                 (build-dom-node :div#custom)
                 (build-dom-node :body)
                 (build-dom-node :html)]]
  (is (= [item history]
         (select-element [:div `CustomType] history)))))


(def bold-class #(html/add-class % "bold"))
(defselector bold-p
  ([:div]
     [[:p] `bold-class]
       #(dalap.html/add-class % "happy")))

#_(def bold-p2
  (let [selectors [[:div]
                    ;;[[:p] bold-class]
                    #(html/add-class % "happy")]]
    (track-visitor
     trackable?
     (selector-visitor
      (mapcat flatten-selectors selectors)
      trackable? html/visit))))

(deftest test-defselector
  (let [result (html/to-html [:div [:p "hello"]]
                             ;;bold-p2
                             (bold-p html/visit)
                             )]
    (is (= (html/to-html [:div.happy [:p.bold "hello"]])
           result))))
