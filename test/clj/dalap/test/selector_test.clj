^{:cljs
  (ns dalap.test.selector-test
   (:require [buster-cljs.core :refer [is]]
            [dalap.selector :refer [-gen-rules-decorator gen-rules-visitor]]
            [dalap.walk :refer [walk]])
   (:require-macros [buster-cljs.macros :refer [deftest it initialize-buster]]))}
(ns dalap.test.selector-test
  (:require [clojure.test :refer [deftest is]]
            [buster-cljs.clojure :refer [it]]
            [dalap.selector :refer [-gen-rules-decorator gen-rules-visitor]]
            [dalap.walk :refer [walk]]
            ))

#_(:cljs (initialize-buster))

(defrecord CustomType [a b])
(defrecord CustomType2 [a b])
(defrecord CustomType3 [a b])
(deftype CustomType4 [a b])

(defn visit-clj-form [form w]
  ;; a modified version of clojure.walk with the ability to drop forms
  (letfn [(filter-map [f form] (remove #(= % (keyword "dalap/form"))
                                         (map f form)))]
    (cond
      (list? form) (apply list (filter-map w form))
      ^:clj (instance? clojure.lang.IMapEntry form)
      ^:clj (vec (filter-map w form))
      (seq? form) (doall (filter-map w form))
      (coll? form) (into (empty form) (filter-map w form))
      :else form)))

(defn assert-walk [visitor input expected & [msg]]
  (let [result (walk input visitor)]
    (is (= result expected)
        (str (and msg (str msg " -- "))
             "expected: " (pr-str expected) " -- "
             "got: " (pr-str result)))))

(deftest test-walk-with-no-rules
  (it "without any rules on visit"
    (let [sample-form '(let [hello "hola"] (str hello))
          visitor (gen-rules-visitor [] visit-clj-form)]

      (assert-walk visitor
                   sample-form sample-form
                   "should be same value"))))

;; rule -> (selector, transformer)

(deftest test-symbol-as-a-selector
  (it "with symbol as a selector on rules"
    (let [transform-rules ['hello 'hallo]
          ;; ^ replace 'hello to 'hallo
          visitor (gen-rules-visitor transform-rules visit-clj-form)]
      (assert-walk visitor
                   '(let [hello "hola"] (str hello))
                   '(let [hallo "hola"] (str hallo))))))

^:clj
(deftest test-class-as-a-selector
  (it "with classes as a selector on rules"
    (let [instance (CustomType. "a" "b")
          replacement-value "Something Else"
          transform-rules [CustomType replacement-value]
          ;; ^ replace CustomType instances with replacement value
          visitor (gen-rules-visitor transform-rules visit-clj-form)]
      (assert-walk visitor
                   [instance]
                   [replacement-value])

      (assert-walk visitor
                   [123 "hello world" [instance "other value"]]
                   [123 "hello world" [replacement-value "other value"]]))))

(deftest test-function-as-a-selector
  (it  "with functions as a selector on rules"
    (let [selector-fn (fn [o w] (vector? o))
          replacement-value "Something Else"
          transform-rules [selector-fn replacement-value]
          ;; ^ replace whenever selector-fn returns true
          ;; with replacement-value
          visitor (gen-rules-visitor transform-rules visit-clj-form)]

      (assert-walk visitor
                   `(["uno" 2] (foobar))
                   `(~replacement-value (foobar))))))

(deftest test-walking-over-a-set
  (it "with set as the collection we are visiting"
    (let [selector 'foo
          replacement-value 999
          transform-rules [selector replacement-value]
          visitor (gen-rules-visitor transform-rules visit-clj-form)]
      (assert-walk visitor
                   #{'foo 'hello}
                   #{999 'hello}
                   "visitor should be able to walk on sets"))))

(deftest test-vector-as-a-selector
  (it "with vectors as selector on rules"
    (let [selector [(fn [n _] (vector? n)) 'foobar]
          replacement-value 'replacement
          transform-rules [selector replacement-value]
          ;; ^ match any `CustomType` instances that are inside a set.
          ;; When using vectors as selectors, it will behave the
          ;; same way as a CSS `parent > child` selector.
          visitor (gen-rules-visitor transform-rules visit-clj-form)]
      (assert-walk visitor
                   [1 2 ['foobar] "other value"]
                   [1 2 ['replacement] "other value"]))))
