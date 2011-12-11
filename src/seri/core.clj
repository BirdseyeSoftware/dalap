(ns seri.core
  (:require [clojure.string :as string]
            [clojure.core.match :as match])
  (:import
   [clojure.lang
    Named ISeq IPersistentVector IPersistentList IPersistentMap PersistentQueue]))

(defprotocol HtmlEscapable
  (toHtmlEscapedStr [x]))

(deftype PreEscaped [s]
  HtmlEscapable (toHtmlEscapedStr [x] s))
(defn safe [& xs] (PreEscaped. (apply str xs)))

(def ^:private html-esc-map {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;"})
(defn- escape-html-char-entities
  "Change special characters into HTML character entities."
  [^String s] (string/escape (str s) html-esc-map))
 
(extend-protocol HtmlEscapable
  ;; no default impl on Object to avoid subtle/silent bugs
  nil (toHtmlEscapedStr [_] "")
  String (toHtmlEscapedStr [s] (escape-html-char-entities s))
  Character (toHtmlEscapedStr [c] (escape-html-char-entities c)))

(defn gen-string-builder
  "Transforms a function that escapes a single string into one that
  will accept multimple args or a seq of strings.

  (gen-string-builder #(. %1 (toString))) yields the equivalent of
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

(def escape-html (gen-string-builder toHtmlEscapedStr))


;;; ;;;;;;;;
(defprotocol Serializable
  "The default implementation of Seri's serialization interface."
  (ser [x walker]))

(extend-protocol Serializable
  ;; Object impl not provided to avoid subtle/silent bugs
  PreEscaped (ser [x _] x)
  nil (ser [_ _] "")
  String (ser [s _] s)
  Character (ser [c _] (str c)) 
  Number (ser [n w] (safe n))           ; optimization
  Named (ser [o w] (str o)))

(defn extend-seri [typ fn]
  (extend typ Serializable {:ser fn}))

(defn extend-seri*
  "Extended version of extend-seri that takes a seq of (type, impl-fn)"
  [types+impl-fns]
  (doseq [[t impl] types+impl-fns]
    (extend-seri t impl)))

(defn- seri-seq-impl [s w] (for [el s] (w el)))
(extend-seri* (for [t [IPersistentVector IPersistentList
                       IPersistentMap PersistentQueue ISeq]]
                [t seri-seq-impl]))

(deftype Walker
    #_"A wrapper around a concrete `serialize` function. It facilitates the
  recursive invocation of `serialize` by providing a single arity
  interface around it. The use of Walker also keeps the signature of
  `serialize` simple as it removes the need to define both single and
  dual arity versions of `serialize` for each Serializable type."

    [serialize-fn]  
    clojure.lang.IFn
    (invoke [this x] (serialize-fn x this))
    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))

(defn serialize           
  "The top level of the API. It walks, flattens and serializes an
  object (or graph of objects) into an escaped String"

  ([x] (serialize x ser escape-html))
  ([x serialize-impl] (serialize x serialize-impl escape-html))
  ([x serialize-impl str-builder]
     (let [walk (Walker. serialize-impl)]
       (apply str-builder (-> [x] walk flatten)))))

;;; xml / html tag serializers 
(def ^{:doc "Regexp for CSS-style id or class in a tag name."
       :private true}
  re-tag  #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(def ^:private maybe-empty-tags
  (set (map name
    '(area base br col frame hr img input link meta p param script))))

(defn- ser-tag-attrs [attrs]
  (for [[k v] attrs]  [" " (name k) (safe \= \") v (safe \")]))

(defn- dom-node [tag attrs content]
  (let [[_ tag id class] (re-matches re-tag (name tag))
        tag-attrs (into {} (filter (fn [[k v]] (not (nil? v)))
                                   {:id id :class class}))
        attrs (merge tag-attrs attrs)
        serialized-attrs (ser-tag-attrs attrs)
        tag-name (name tag)
        is-empty (and (maybe-empty-tags tag) (empty? content))
        sf safe]
    [(sf \<) tag-name serialized-attrs (if is-empty (sf " />") (sf \>))
     (if-not is-empty [content (sf "</") tag-name (sf \>)])]))

(defn ser-vector [v w]
  (let [named? (fn [x] (instance? Named x))]
    (match/match
     [v] 
     [([(tag :when named?) (attrs :when map?) & content] :seq)]
     (w (dom-node tag attrs content))
 
     [([(tag :when named?) & content] :seq)]
     (w (dom-node tag {} content))

     [_] (seri-seq-impl v w))))

(extend-seri clojure.lang.IPersistentVector ser-vector)

;;; test
(serialize [" match tag: " [:p#asdf "Paragraph&" [1 2 "34" 'bar] " Foo"]
            " match tag: " [:p#fdsf.oiu {:id "bar"} "Paragraph" " Foo"]
            " empty :" [:hr.clear {:asdf 1234}]
            ])

#_(serialize ["safe: " (apply safe (keys html-esc-map))
            " chars to esc: " (keys html-esc-map)
            (apply str (keys html-esc-map))
            " numbers: " 1 2 3.0 
            " vec: " ["->-"]
            " list: " '(1 2 3)
            " seq: " (seq [1 2 3])
            " nil: " nil
            " symbol: " 'Foo
            " maps: " {:a 1}
            " keywords: " :a " " :foo/b
            ])

