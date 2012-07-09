(ns dalap.test.escape
  (:use [clojure.test])
  (:require [dalap.escape :as core]
            [dalap.escape.html :as html]))

(deftest test-gen-str-escaper
  (is (= ((core/-gen-str-escaper #(. %1 (toString))) "abc" 123)
         "abc123")
      "This should behave the same as `clojure.core/str`")
  (is (= ((core/-gen-str-escaper #(.toUpperCase (.toString %))) "abc" 123)
         "ABC123")
      "uppercase version of str"))

(deftest test-low-level-char-escaping
  (doseq [[k v] html/html-escaping-map]
    [(is (= (html/-escape-html-chars k) v)
         "test lower-level escaper")
     (is (= (html/escape-html (str k)) v)
         "test via HtmlEscapable protocol")]))

(deftest test-HtmlEscapable
  (doseq [[inp outp] {
                      nil ""
                      (core/safe "&") "&"
                      (core/safe \&) "&"
                      (core/safe "&-<->-\"") "&-<->-\""
                      \& "&amp;"
                      "abcd&e" "abcd&amp;e"
                      "&-<->-\"" "&amp;-&lt;-&gt;-&quot;"
                      }]
    (is (= (html/-to-html-escaped-str inp)
           (html/escape-html inp)
           outp))))

(deftest test-mixed-pre-escaped-and-not
  (is (= (html/escape-html (core/safe "&-<->-\"") "-&->")
         "&-<->-\"-&amp;-&gt;")))

