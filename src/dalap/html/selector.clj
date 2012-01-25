(ns dalap.html.selector
  (:use [clojure.core.match :only (match)])

  (:use [dalap.walk :only [update-in-state
                           wrap-walker
                           compose-visitors]])
  (:require [dalap.html :as html])
  (:import [dalap.html DomNode]))

;;; NOTE: all the dynamically created functions (from `fn`) in this
;;; module are given names to aid in debugging

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

(defn- dom-matches-tag-selector?
  "Tells if a DomNode instance matches a given selector."
  [dom-node tag-selector]
  (let [[_ tag id class] (re-matches html/re-tag (name tag-selector))]
    (and (html/has-tag-name? dom-node tag)
         (if (nil? id)
           true
           (html/has-id? dom-node id))
         (if (nil? class)
           true
           (html/has-class? dom-node class)))))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Node/Location selectors

(defprotocol NodeSelectorSpec
  (adapt-to-node-selector [spec]))

(extend-protocol NodeSelectorSpec
  clojure.lang.Keyword
  (adapt-to-node-selector [dom-node-kw]
    (fn dom-node-matcher [node]
      (if (html/dom-node? node)
        (dom-matches-tag-selector? node dom-node-kw))))

  clojure.lang.IFn
  (adapt-to-node-selector [sfn] sfn)

  Object
  (adapt-to-node-selector [type_]
    (fn type-matcher [node]
      (= (type node) type_))))

(defprotocol LocationSelectorSpec
  (adapt-to-location-selector [spec]))

(extend-protocol LocationSelectorSpec
  clojure.lang.PersistentVector
  (adapt-to-location-selector [selector-vec]
    (let [selector (map adapt-to-node-selector selector-vec)]
      (fn location-matcher [history-stack]
        (matching-node selector history-stack))))

  clojure.lang.IFn
  (adapt-to-location-selector [sfn] sfn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn arg-count [f]
  ;; for cljs see js' function.length
  (let [m (first (.getDeclaredMethods (class f)))
        p (.getParameterTypes m)]
    (alength p)))

(defprotocol VisitorSpec
  (adapt-to-visitor [spec]))

(extend clojure.lang.IPersistentMap VisitorSpec
        ;; for some reason interfaces have to be extended like this
        ;; rather than via extend-protocol
        {:adapt-to-visitor
         (fn [m] (fn map-visitor [node _w] (m node)))})

(defn- adapt-fn-to-visitor [vfn]
    (case (arg-count vfn)
    0 (fn zero-arg-fn-visitor [_node _w] (vfn))
    1 (fn single-arg-fn-visitor [node _w] (vfn node))
    ;; else
    vfn))
(extend clojure.lang.IFn VisitorSpec
        {:adapt-to-visitor adapt-fn-to-visitor})

(extend-protocol VisitorSpec
  clojure.lang.Keyword
  (adapt-to-visitor [k]
    (fn map-visitor [node _w] (k node)))

  ;; make sure vectors don't get treated as functions
  clojure.lang.PersistentVector
  (adapt-to-visitor [v]
    (fn replacement-value-visitor [_node _w] v))

  Object
  (adapt-to-visitor [obj]
    (fn replacement-value-visitor [_node _w] obj)))

;;;
(defn- gen-visitor-from-pred-visitor-pairs
  [predicates+visitors inspect-node?]
  (fn predicate-table-visitor [node walker]
    (or (and (inspect-node? node)
             (let [history-stack (:history walker)]
               (some (fn sequential-pred-checker [[pred? visitor]]
                       (if (pred? history-stack)
                         (visitor node walker)))
                   predicates+visitors)))
        node)))

(defn compile-selector-visitor-pairs [selectors+visitors]
  (for [[sel vis] selectors+visitors]
    [(adapt-to-location-selector sel)
     (adapt-to-visitor vis)]))

(defn gen-decorator
  "Creates a visitor function decorator that will apply the provided
  visitor to any node that matches its corresponding selector.

  Example:
   (def add-class-to-p (gen-decorator
    [[:div.interesting :p] #(dalap.html/add-class % \"bold\")])
   (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+visitors & [paired?]]

  (let [inspect-node? identity ;; track/match-on everything except nil/false
        pairs (if paired? selectors+visitors (partition 2 selectors+visitors))
        inner-visitor (gen-visitor-from-pred-visitor-pairs
                       (compile-selector-visitor-pairs pairs)
                       inspect-node?)
        add-history-to-walker (fn add-hist [node w]
                                (if (inspect-node? node)
                                  (update-in-state w :history #(conj % node))
                                  w))]
    (fn decorator [visit-fn]
      (wrap-walker (compose-visitors inner-visitor visit-fn)
                   add-history-to-walker))))
