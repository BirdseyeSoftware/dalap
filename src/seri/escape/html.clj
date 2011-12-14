(ns seri.escape.html

;; Third Party ;;;;;

  (:require [clojure.string :as string])

;; Local Library ;;;

  (:use seri.escape)
  (:import [seri.escape PreEscaped]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def html-esc-map {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;"})
(defn escape-html-char-entities
  [^String s] (string/escape (str s) html-esc-map))

(defprotocol HtmlEscapable
  (toHtmlEscapedStr [x]))

(extend-protocol HtmlEscapable
  ;; no default impl on Object to avoid subtle/silent bugs
  PreEscaped (toHtmlEscapedStr [x] (str x))
  nil (toHtmlEscapedStr [_] "")
  String (toHtmlEscapedStr [s] (escape-html-char-entities s))
  Character (toHtmlEscapedStr [c] (escape-html-char-entities c)))

(def escape-html (gen-str-escaper toHtmlEscapedStr))

