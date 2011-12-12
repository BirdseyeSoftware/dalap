(ns seri.escape
  (:require [clojure.string :as string]))

(deftype PreEscaped [val]
  Object
  (toString [_] (str val)))

(defn safe [& xs] (PreEscaped. (apply str xs)))

(defn gen-str-escaper
  "Transforms a function that escapes a single string into one that
  will accept multimple args or a seq of strings.

  (gen-str-escaper #(. %1 (toString))) yields the equivalent of
  `clojure.core.str`"
  [normalize-single-string]
  (fn ^String str**
    (^String [] "")
    (^String [^Object x] (normalize-single-string x))
    (^String [x & ys]
             ((fn [^StringBuilder sb more]
                (if more
                  (recur (. sb (append (str** (first more)))) (next more))
                  (str sb)))
              (new StringBuilder (str** x)) ys))))

;;; html specific

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
