(ns seri.html
  (:use [seri.escape :only [safe escape-html html-esc-map]])
  (:require [seri.defaults]
            [seri.walk]
            [clojure.core.match :as match])
  (:import [seri.escape PreEscaped]))

(defprotocol HtmlSerializable
  (visit [x w]))

(extend-protocol HtmlSerializable
  Object (visit [n w] (seri.defaults/visit n w))
  nil (visit [_ _] "")
  Number (visit [n w] (safe (str n)))
  PreEscaped (visit [x _] x))

(def ^{:doc "Regexp for CSS-style id or class in a tag name."
       :private true}
  re-tag  #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(def ^:private maybe-empty-tags
  (set (map name
    '(area base br col frame hr img input link meta p param script))))

(deftype TagAttrs [attrs-map]
  HtmlSerializable
  (visit [_ w] (w (for [[k v] attrs-map]
                    [" " (name k) (safe \= \") v (safe \")]))))

(defn- dom-node [tag attrs content]
  (let [[_ tag id class] (re-matches re-tag (name tag))
        attrs (TagAttrs.
               (merge (into {} (filter (fn [[k v]] (not (nil? v)))
                                       {:id id :class class}))
                      attrs))
        tag-name (name tag)
        is-empty (and (maybe-empty-tags tag) (empty? content))
        sf safe] 
    [(sf \<) tag-name attrs (if is-empty (sf " />") (sf \>))
     (if-not is-empty [content (sf "</") tag-name (sf \>)])]))


(defn- named? [x] (instance? clojure.lang.Named x))
(defn visit-vector [v w]
  (let [named? (fn [x] (instance? Named x))]
    (match/match
     [v] 
     [([(tag :when named?) (attrs :when map?) & content] :seq)]
     (w (dom-node tag attrs content))
 
     [([(tag :when named?) & content] :seq)]
     (w (dom-node tag {} content))

     [_] (seri.defaults/visit-seq v w))))

(extend clojure.lang.IPersistentVector HtmlSerializable {:visit visit-vector})

(defn to-html
  [x] (apply escape-html (flatten (seri.walk/walk x visit))))

;;; test
(to-html [" match tag: " [:p#asdf "Paragraph&" [1 2 "34" 'bar] " Foo"]
          " match tag: " [:p#fdsf.oiu {:id "bar"} "Paragraph" " Foo"]
          " empty :" [:hr.clear {:asdf 1234}]

          "safe: " (apply safe (keys html-esc-map))
          " chars to esc: " (keys html-esc-map)
          (apply str (keys html-esc-map))            
          ])
