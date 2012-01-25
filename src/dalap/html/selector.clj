(ns dalap.html.selector
  (:use [clojure.core.match :only (match)])

  (:use [dalap.walk :only [update-in-state
                           wrap-walker
                           compose-visitors]])
  (:require [dalap.html :as html])
  (:import [dalap.html DomNode]))

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

(defn- compile-selector
  "Creates a node matching predicate from a single selector."
  [single-selector]
  (cond
    (keyword? single-selector)
    (fn [node]
      (if (html/dom-node? node)
        (dom-matches-tag-selector? node single-selector)))

    (fn? single-selector)
    single-selector

    :else
    (fn [node]
      (= (type node) single-selector))))

(defn- compile-selector* [selector]
  (cond
    (vector? selector)
    (let [selector (map compile-selector selector)]
      (fn [history-stack]
        (matching-node selector history-stack)))

    (fn? selector)
    selector

    ;;:else
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn arg-count [f]
  (let [m (first (.getDeclaredMethods (class f)))
        p (.getParameterTypes m)]
    (alength p)))

(defn- gen-visitor-from-pred-visitor-pairs
  [predicates+visitors inspect-node?]
  (fn [node walker]
    (or (and (inspect-node? node)
             (let [history-stack (:history walker)]
               (some (fn [[pred? visitor]]
                     (if (pred? history-stack)
                       (visitor node walker)))
                   predicates+visitors)))
        node)))

(defn normalize-visitor [visitor]
  (if (fn? visitor)
    (case (arg-count visitor)
      0 (fn [_node _w] (visitor))
      1 (fn [node _w] (visitor node))
      2 visitor)
    ;; else it's straigh-up replacement value rather than a func
    (fn [_node _w] visitor)))

(defn compile-selector-visitor-pairs [selectors+visitors]
  (for [[sel vis] selectors+visitors]
    [(compile-selector* sel) (normalize-visitor vis)]))

(defn gen-decorator
  "Creates a visitor function decorator that will apply the provided
  visitor to any node that matches its corresponding selector.

  Example:
   (def add-class-to-p (gen-decorator
    [[:div.interesting :p] #(dalap.html/add-class % \"bold\")])
   (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+visitors & [paired?]]

  (let [inspect-node? identity ;; track everything except nil/false
        pairs (if paired? selectors+visitors (partition 2 selectors+visitors))
        inner-visitor (gen-visitor-from-pred-visitor-pairs
                       (compile-selector-visitor-pairs pairs)
                       inspect-node?)
        add-history-to-walker (fn [node w]
                                (if (inspect-node? node)
                                  (update-in-state w :history #(conj % node))
                                  w))]
    (fn [visit-fn]
      (wrap-walker (compose-visitors inner-visitor visit-fn)
                   add-history-to-walker))))
