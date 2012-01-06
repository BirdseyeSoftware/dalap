(ns dalap.test.html
  (:use [clojure.test])
  (:use [dalap.html :only [to-html]])
  (:require [dalap.html :as html]))

(def basic-sample-data
  [1 2 3 "abc"
   \x \space 1.2
   " " 'foo :bar
   nil nil [nil]])

(def basic-sample-data-str "123abcx 1.2 foo:bar")

(defn- assert-html
  ([in out] (assert-html in out nil))
  ([in out msg] (is (= (to-html in) out) msg)))

(deftest test-basic-types
  (assert-html basic-sample-data basic-sample-data-str)

  (assert-html ["" :ti&tle "fo\"o"] ":ti&amp;tlefo&quot;o"
               "the keyword above should be HTML escaped"))

(deftest test-vector-tag-dsl
  (assert-html [:hr#id] "<hr id=\"id\" />")
  (assert-html [:hr.classA] "<hr class=\"classA\" />")
  (assert-html [:hr#id.cls] "<hr id=\"id\" class=\"cls\" />")
  (assert-html [:div [:div basic-sample-data]]
               (str "<div><div>" basic-sample-data-str "</div></div>"))
  (assert-html [:hr.clear {:asdf 1234}]
               "<hr asdf=\"1234\" class=\"clear\" />")
  (assert-html [:input {:type "text"}] "<input type=\"text\" />")
  (assert-html [:div [:div [:div [:div]]]]
               "<div><div><div><div></div></div></div></div>")
  (assert-html [:div#a [:div#b [:div#c [:div#d]]]]
               (str
                "<div id=\"a\"><div id=\"b\"><div id=\"c\"><div id=\"d\">"
                "</div></div></div></div>"))
  (assert-html [:form#contact {:action "GET"}
                [:input {:type "text"
                         :name "first-name"
                         :value "Tavis & Roman"}]]
               (str
                "<form action=\"GET\" id=\"contact\"><input value=\"Tavis &amp;"
                " Roman\" type=\"text\" name=\"first-name\" /></form>")))
