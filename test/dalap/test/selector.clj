(ns dalap.test.selector
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [dalap.selector :refer [-gen-decorator gen-visitor]]
            [dalap.walk :refer [walk]]))

(defrecord CustomType [a b])
(defrecord CustomType2 [a b])
(defrecord CustomType3 [a b])
(deftype CustomType4 [a b])

(defn visit-clj-form [form w]
  ;; a modified version of clojure.walk with the ability to drop forms
  (letfn [(filter-map [f form] (remove #(= % :dalap/drop-form)
                                       (map f form)))]
    (cond
      (list? form) (apply list (filter-map w form))
      (instance? clojure.lang.IMapEntry form) (vec (filter-map w form))
      (seq? form) (doall (filter-map w form))
      (coll? form) (into (empty form) (filter-map w form))
      :else form)))

(defn assert-walk [visitor input expected]
  (is (= (walk input visitor)
         expected)))

(deftest test-identity-visitor
  (let [sample-form
        '(let [hello "hola"] (str hello))
        visitor (gen-visitor [] visit-clj-form)]
    (assert-walk visitor sample-form sample-form)))

;; rule -> (selector, transformer)

(deftest test-symbol-as-a-selector
  (let [transform-rules ['hello 'hallo]
        ;; ^ replace 'hello to 'hallo
        visitor (gen-visitor transform-rules visit-clj-form)]
    (assert-walk visitor
                '(let [hello "hola"] (str hello))
                '(let [hallo "hola"] (str hallo)))))

(deftest test-class-as-a-selector
  (let [instance (CustomType. "a" "b")
        replacement-value "Something Else"
        transform-rules [CustomType replacement-value]
        ;; ^ replace CustomType instances with replacement value
        visitor (gen-visitor transform-rules visit-clj-form)]

    (assert-walk visitor
                 [instance]
                 [replacement-value])

    (assert-walk visitor
                 [123 "hello world" [instance "other value"]]
                 [123 "hello world" [replacement-value "other value"]])))

(deftest test-function-as-a-selector
  (let [selector-fn (fn [o w] (vector? o))
        replacement-value "Something Else"
        transform-rules [selector-fn replacement-value]
        ;; ^ replace whenever selector-fn returns true
        ;; with replacement-value
        visitor (gen-visitor transform-rules visit-clj-form)]

    (assert-walk visitor
                 `(["uno" 2] (foobar))
                 `(~replacement-value (foobar)))))

(deftest test-vector-as-a-selector
  (let [selector [set? CustomType]
        replacement-value 'custom-type-inside-set
        transform-rules [selector replacement-value]
        ;; match any CustomTypes instances that are inside a set.
        ;; When using vectors as selectors, it will behave the
        ;; same way as a CSS `parent > child` selector.
        visitor (gen-visitor transform-rules visit-clj-form)]
    (assert-walk visitor
                 [1 2 #{(CustomType. "a" "b")} "other value"]
                 [1 2 #{'custom-type-inside-set} "other value"])))
