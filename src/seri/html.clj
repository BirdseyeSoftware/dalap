(ns seri.html
  (:import [clojure.lang IPersistentVector])

  (:require [clojure.core.match :as match])

  (:use [seri.escape :only [safe]]
        [seri.escape.html :only [escape-html]])
  (:require [seri.defaults]
            [seri.walk])
  (:import [seri.escape PreEscaped]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
    '(area base br col frame hr img input link meta p param))))

(deftype TagAttrs [tag attrs-map]
  HtmlSerializable
  (visit [_ w] (w (for [[k v] attrs-map]
                    [" " (name k) (safe \= \") v (safe \")]))))

(deftype DomNode [tag attrs content]
  HtmlSerializable
  (visit [_ w]
    (let [[_ tag id class] (re-matches re-tag (name tag))
          tag-name (name tag)
          attrs (TagAttrs.
                 tag
                 (merge (into {} (filter (fn [[k v]] (not (nil? v)))
                                         {:id id :class class}))
                        attrs))
          is-empty (and (maybe-empty-tags tag) (empty? content))
          open-tag [(safe \<) tag-name attrs
                    (if is-empty (safe " />") (safe \>))]
          close-tag [(safe "</") tag-name (safe \>)]]
      (w [open-tag (if-not is-empty [content close-tag])]) )))

(defn visit-vector [v w]
  (let [named? (fn [x] (instance? clojure.lang.Named x))]
    (match/match [v]
      [([(tag :when named?) (attrs :when map?) & content] :seq)]
      (w (DomNode. tag attrs content))

      [([(tag :when named?) & content] :seq)]
      (w (DomNode. tag {} content))

      [_] (seri.defaults/visit-seq v w))))

(extend IPersistentVector HtmlSerializable {:visit visit-vector})

(defn to-html
  ([x] (to-html x visit))
  ([x visitor-fn]
     (apply escape-html (flatten (seri.walk/walk x visitor-fn)))))

;;; helpers
(def doctype
  {:html4
   (safe "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" "
        "\"http://www.w3.org/TR/html4/safeict.dtd\">\n")
   :xhtml-safeict
   (safe "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Safeict//EN\" "
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-safeict.dtd\">\n")
   :xhtml-transitional
   (safe "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
   :html5
   (safe "<!DOCTYPE html>\n")})

(defn html5 [& contents]
  [(doctype :html5)
   [:html ;{:lang (options# :lang)}
    contents]])

;;; test

#_(to-html
   [" match tag: " [:p "Paragraph&" [1 2 "34" 'bar] " Foo"]
    " match tag: " [:p {:id "bar" :ti&tle "fo\"o"} "Paragraph" " Foo"]
    " nesting: " [:div [:div [:div [:div]]]]
    " match tag: " [:input]
    " match tag: " [:input {:type "text"}]
    " match tag: " [:form#contact {:action "GET"}
                    [:input {:type "text"
                             :name "first-name"
                             :value "Tavis & Company"}]]
    " empty :" [:hr.clear {:asdf 1234}]
    ;" safe: " (apply safe (keys html-esc-map))
    ;" chars to esc: " (keys html-esc-map)
    ;(apply str (keys html-esc-map))
    ])
