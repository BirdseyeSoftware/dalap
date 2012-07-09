(ns dalap.escape.html
  (:require [clojure.string :as string]
            [dalap.escape :refer (gen-str-escaper)])
  (:import [dalap.escape PreEscaped]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce
  ^{:doc "Char -> String (HTML escaped representation)
         API: Public 

          Map of special HTML characters to it's _escaped_ string
          representation"}

  html-escaping-map {
    \& "&amp;"
    \< "&lt;"
    \> "&gt;"
    \" "&quot;"
  })

(defn -escape-html-chars
  "Object -> String (HTML escaped)
  API: Internal

  Escapes string's characters that are contained in the
  `dalap.escape.html/html-escaping-map`, in case an object
  is given, the `clojure.core/str` will be applied to it."
  [s]
  (string/escape (str s) html-escaping-map))

(defprotocol HtmlEscapable
  (-to-html-escaped-str
    [x]
    "dalap.escape.html/HtmlEscapable -> String (HTML escaped)
    API: Internal

    Transforms a record into a String. The resulting string
    _must be_ HTML escaped. In order to implement this functions for your
    objects we recommed using the `dalap.escape.html/-escape-html-chars`
    function.

    This function _should not_ be called directly, use
    `dalap.escape.html/escape-html` function instead."))

(extend-protocol HtmlEscapable
  ;; Object (toHtmlEscapedStr [_] "")
  ;; ^ we avoid having a default implementation on Object
  ;; to not have subtle/silent bugs
  PreEscaped
    (-to-html-escaped-str [x] (str x))

  nil
    (-to-html-escaped-str [_] "")

  String
    (-to-html-escaped-str [s]
      (-escape-html-chars s))

  Character
    (-to-html-escaped-str [c]
      (-escape-html-chars c)))

(defonce
  ^{:doc "`dalap.escape.html/HtmlEscapable -> String (HTML escaped)
         API: Public

         Transforms a record that implements the HtmlEscapable protocol
         into a HTML escaped String"}
  escape-html (gen-str-escaper -to-html-escaped-str))
