(ns dalap.test.escape
  (:use [clojure.test])

  (:require [dalap.escape :as core]
            [dalap.escape.html :as html]))

(deftest test-gen-str-escaper
  (is (= ((core/gen-str-escaper #(. %1 (toString))) "abc" 123)
         "abc123")
      "This should behave the same as `clojure.core/str`")
  (is (= ((core/gen-str-escaper #(.toUpperCase (.toString %))) "abc" 123)
         "ABC123")
      "This should behave the same as `clojure.core/str`"))

(deftest test-html-char-escaping
  (doseq [[k v] html/html-esc-map]
    [(is (= (html/escape-html-char-entities k) v))
     (is (= (html/escape-html (str k)) v))]))

(deftest test-html-string-escaping
  (is (= (html/escape-html "&-<->-\"")
         "&amp;-&lt;-&gt;-&quot;")))

(deftest test-html-pre-escaped
  (is (= (html/escape-html (core/safe "&-<->-\""))
         "&-<->-\""))
  (is (= (html/escape-html (core/safe "&-<->-\"") "-&->")
         "&-<->-\"-&amp;-&gt;")))
