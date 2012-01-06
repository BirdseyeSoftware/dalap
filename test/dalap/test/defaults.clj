(ns dalap.test.defaults
  (:use [clojure.test])

  (:use [dalap.walk :only [walk]])
  (:use [dalap.defaults :only [serialize visit]]))

(def basic-sample-data
  [1 2 3 "abc"
   \x \space 1.2
   " " 'foo :bar
   nil nil [nil]])

(def basic-sample-data-str "123abcx 1.2 foo:bar")

(defmacro assert-serialized
  ([in out] `(assert-serialized ~in ~out nil))
  ([in out msg] `(is (= (serialize ~in) ~out) ~msg)))

(deftest test-basic-types
  (assert-serialized {:a 1} ":a1")
  (assert-serialized nil "")
  (assert-serialized 'Foo "Foo")

  (assert-serialized basic-sample-data basic-sample-data-str)
  ;; these contained forms should all be equivalent
  (is (apply =
             (for [input [basic-sample-data
                          (seq [[[basic-sample-data]]])
                          (list (list basic-sample-data))
                          (for [el basic-sample-data] el)
                          basic-sample-data-str]]
               (serialize input))))

  (doseq [el basic-sample-data]
    (if (not (sequential? el))
      (is (=
           (serialize el)
           (serialize [el])
           (visit el nil)
           (str el))))))
