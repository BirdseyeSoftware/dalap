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

(deftest test-basic-types
  (is (= (to-html basic-sample-data)
         basic-sample-data-str))
  (is (= (to-html ["" :ti&tle "fo\"o"])
         ":ti&amp;tlefo&quot;o")))

(deftest test-vector-tag-dsl
  (is (= (to-html [:hr#id])
         "<hr id=\"id\" />"))

  (is (= (to-html [:hr.classA])
         "<hr class=\"classA\" />"))

  (is (= (to-html [:hr#id.cls])
         "<hr id=\"id\" class=\"cls\" />"))

  (is (= (to-html [:div [:div basic-sample-data]])
         (str "<div><div>" basic-sample-data-str "</div></div>")))

  (is (= (to-html [:hr.clear {:asdf 1234}])
         "<hr asdf=\"1234\" class=\"clear\" />"))
  (is (= (to-html [:input {:type "text"}]) "<input type=\"text\" />"))
  (is (= (to-html [:div [:div [:div [:div]]]])
         "<div><div><div><div></div></div></div></div>"))
  (is (= (to-html [:div#a [:div#b [:div#c [:div#d]]]])
         (str
          "<div id=\"a\"><div id=\"b\"><div id=\"c\"><div id=\"d\">"
          "</div></div></div></div>")))
  (is (= (to-html [:form#contact {:action "GET"}
                   [:input {:type "text"
                            :name "first-name"
                            :value "Tavis & Roman"}]])
         (str
          "<form action=\"GET\" id=\"contact\"><input value=\"Tavis &amp;"
          " Roman\" type=\"text\" name=\"first-name\" /></form>"))))
