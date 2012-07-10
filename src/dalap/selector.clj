(ns dalap.selector
  (:require [clojure.core.match :refer (match)]
            [dalap.html :as html]
            [dalap.walk :refer
              (update-in-state
               wrap-walker
               compose-visitors)])
  (:import [dalap.html DomNode]))

;; NOTE: all the dynamically created functions (from `fn`) in this
;; module are given names to aid in debugging

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol NodeMatcher
  "This protocol provides a function that returns another
  function that accepts a node from the dalap Tree and tests
  it matches."
  (to-node-matcher [spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- dom-matches-tag-selector?
  "Tests if a DomNode instance matches a given tag-selector."
  [dom-node tag-selector]
  (let [[_ tag id class] (re-matches html/re-tag (name tag-selector))]
    (and (html/has-tag-name? dom-node tag)
         (if (nil? id)
           true
           (html/has-id? dom-node id))
         (if (nil? class)
           true
           (html/has-class? dom-node class)))))


(extend-protocol NodeMatcher
  ;; On Keywords it will try to match to DomNode instances.
  clojure.lang.Keyword
  (to-node-matcher [dom-node-kw]
    (fn dom-node-matcher [node]
      (if (html/dom-node? node)
        (dom-matches-tag-selector? node dom-node-kw))))

  ;; Something that behaves like function will always match
  ;; and return itself.
  clojure.lang.IFn
  (to-node-matcher [sfn] sfn)

  ;; Anything else, it will check the dalap node type is
  ;; equal to the object.
  Object
  (to-node-matcher [type_]
    (fn type-matcher [node]
      (= (type node) type_))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol TreeLocMatcher
  "Provides `to-tree-loc-matcher`, which converts a selector `spec`
  into a matcher predicate that matches locations in a dalap input
  tree. It can match on the type or attributes of the current node or,
  optionally, also match on the parents of the current
  node.

  The signature of the generated matcher function is [node, walker],
  which is the same signature as dalap visitors. Any non-nil /
  non-false return value is considered a match."
  (to-tree-loc-matcher [spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- span [p xs]
  ((juxt (partial take-while p)
         (partial drop-while p)) xs))

(defn match-selector
  "Grabs a node from history that matches the given
  selector."
  [select? history]
  (let [[new-history [node & _]] (span (complement select?) history)]
    (if (nil? node)
      [nil history]
      [node new-history])))

(defn match-selector*
  "Multiple selector version of match-selector.

  Grabs an node from history that matches a group
  of selectors, respecting a heriarchy of multiple nodes
  in between."
  [selectors history]
  (loop [current-history  history
         current-selector (first selectors)
         rest-selectors   (rest selectors)]
    (let [[node new-history]
          (match-selector current-selector current-history)]
      (cond
        (nil? node) [nil history]

        (and (empty? rest-selectors)
             (not (empty? new-history)))
        [nil history]

        (empty? rest-selectors)
        [node history]

        :else
        (recur new-history
               (first rest-selectors)
               (rest rest-selectors))))))

(defn- matching-node [selector history]
  (first (match-selector* selector history)))

(extend-protocol TreeLocMatcher
  ;; For vectors, check that each element
  ;; matches in the history-stack using the NodeMatcher
  ;; impl of each given selector.
  clojure.lang.PersistentVector
  (to-tree-loc-matcher [selector-vec]
    (let [selector (map to-node-matcher selector-vec)]
      (fn location-matcher [_node walker]
        (let [history-stack (:history walker)]
          (matching-node selector history-stack)))))

  clojure.lang.Symbol
  (to-tree-loc-matcher [sym]
    (fn [node _walker] (= node sym)))

  java.lang.Class
  (to-tree-loc-matcher [kls]
    (fn [node _walker] (= (type node) kls)))

  ;; Match if the given function returns true.
  clojure.lang.IFn
  (to-tree-loc-matcher [sfn] sfn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IVisitorPrototype
  "Provides a function for types to create a dalap visitor function."

  (to-visitor [prototype]
    "Signature: [VisitorPrototype] -> Visitor Function
    API: Public

    Creates a dalap visitor from a record."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; For Maps it checks if the current node is a key of the
;; given map.
;(extend clojure.lang.IPersistentMap IVisitorPrototype
;  ;; for some reason interfaces have to be extended like this
;  ;; rather than via extend-protocol
;  {:to-visitor
;    (fn [m] (fn map-visitor [node _w] (m node)))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn arg-count [f]
;  ;; for cljs see js' function.length
;  (let [m (first (.getDeclaredMethods (class f)))
;        p (.getParameterTypes m)]
;    (alength p)))
;
;(defn- ifn-to-visitor [vfn]
;    (case (arg-count vfn)
;    0 (fn zero-arg-fn-visitor [_node _w] (vfn))
;    1 (fn single-arg-fn-visitor [node _w] (vfn node))
;    ;; else
;    vfn))
;
;;; For function-like types it will just call them with
;;; the current node
;(extend clojure.lang.IFn IVisitorPrototype
;        {:to-visitor ifn-to-visitor})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IVisitorPrototype

  clojure.lang.IPersistentMap
  ;; For maps, it checks if the current node is a key of the
  ;; given map.
  (to-visitor [m]
    (fn map-visitor [node _w] (m node)))

  clojure.lang.IFn
  ;; For function-like types it will just call them with the current
  ;; node
  (to-visitor [vfn]
    (letfn [(arg-count [f]
              (let [m (first (.getDeclaredMethods (class f)))
                    p (.getParameterTypes m)]
                (alength p)))]
      (case (arg-count vfn)
        0 (fn zero-arg-fn-visitor [_node _w] (vfn))
        1 (fn single-arg-fn-visitor [node _w] (vfn node))
        vfn)))

  clojure.lang.Keyword
  ;; Keywords will be called as functions on the nodes of the dalap tree.
  (to-visitor [k]
    (fn keyword-replacement-value-visitor [node _walker] (k node)))

  clojure.lang.PersistentVector
  ;; PersistentVectors replace the current node of the dalap tree
  ;; with themselves.
  ;;
  ;; This would match the IFn implementation if this more specific one
  ;; were not provided here.
  (to-visitor [v]
    (fn vec-replacement-value-visitor [_node _walker] v))

  clojure.lang.Symbol
  ;; This would match the IFn implementation if this more specific one
  ;; were not provided here.
  (to-visitor [sym]
    (fn symbol-replacement-value-visitor [_node _walker] sym))

  Object
  ;; All other Objects replace the current node of the dalap tree
  ;; with themselves.
  (to-visitor [obj]
    (fn replacement-value-visitor [_node _walker] obj)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- gen-visitor-from-pred-visitor-pairs
  [predicates+visitors inspect-node?]
  (fn predicate-table-visitor [node walker]
    ;; this is becoming more and more like a dynamically created
    ;; multimethod and should probably be converted into one
    ;; In the simple case where only the current node is used for
    ;; dispatch and not the walk history, this can be optimized into a
    ;; dynamically created Protocol.
    (if (inspect-node? node)
      (let [vis (or ; use the visitor for the first matching predicate
                 (some (fn pred-checker [[p? v]] (if (p? node walker) v))
                       predicates+visitors)
                 ;; or fall through
                 (constantly node))]
        (vis node walker))
      node)))

(defn normalize-selector-transformer-pairs [selectors+transformers]
  (for [[sel transformer] selectors+transformers]
    [(to-tree-loc-matcher sel)
     (to-visitor transformer)]))

(defn ^{:api :internal} -gen-decorator
  "API: Internal

  Creates a visitor function decorator that will apply the provided
  visitor to any node that matches its corresponding selector.

  Example:
    (def add-class-to-p
         (-gen-decorator [[:div.interesting :p]
                           #(dalap.html/add-class % \"bold\")

                          [:div.other :p]
                           #(dalap.html/add-class % \"italics\")]))

    (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+transformers]
  (let [inspect-node? identity
        ;; ^ track/match-on everything except nil/false
        ;; TODO: it might better to do (constantly true) rather than identity
        pairs (partition 2 selectors+transformers)
        inner-visitor (gen-visitor-from-pred-visitor-pairs
                        (normalize-selector-transformer-pairs pairs)
                        inspect-node?)
        add-history-to-walker (fn add-hist [node w]
                                (if (inspect-node? node)
                                  (update-in-state w :history #(conj % node))
                                  w))]
    (fn decorator [visit-fn]
      (wrap-walker (compose-visitors inner-visitor visit-fn)
                   add-history-to-walker))))


(defn ^{:api :public} gen-visitor
  "API: Public"
  ([selectors+transformers]
    (gen-visitor selectors+transformers dalap.defaults/visit))
  ([selectors+transformers fallback-visitor]
    ((-gen-decorator selectors+transformers) fallback-visitor)))


