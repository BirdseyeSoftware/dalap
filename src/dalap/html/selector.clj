(ns dalap.html.selector
  (:use [clojure.core.match :only (match)])

  (:use [dalap.walk :only (update-in-state)])
  (:require [dalap.html :as html])
  (:import [dalap.html DomNode]))

(defn- span [p xs]
  ((juxt (partial take-while p)
         (partial drop-while p)) xs))

(defn dom-matches-selector?
  "Tells if a DomNode instance matches a given selector."
  [dom-node selector]
  (let [[_ tag id class] (re-matches html/re-tag (name selector))]
    (and (html/has-tag-name? dom-node tag)
         (if (nil? id)
           true
           (html/has-id? dom-node id))
         (if (nil? class)
           true
           (html/has-class? dom-node class)))))

(defn- gen-matcher-pred
  "Creates a node matching predicate from a single selector.

  Does a span using dom-matches-selector? on nodes
  that are DomNode type, and compares types otherwise."
  [single-selector]
  (fn [node]
    (cond
      (html/dom-node? node)
      (not (dom-matches-selector? node single-selector))
      :else
      (not (= (symbol (.getName (type node))) single-selector)))))

(defn match-selector
  "Grabs a node from history that matches the given
  selector."
  [selector history]
  (let [match-node? (gen-matcher-pred selector)
        [new-history [node & _]] (span #(match-node? %) history)]
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

(defn- selector-to-predicate [selector]
  (cond
    (fn? selector)
    selector

    :else
    (fn [history-stack]
    (matching-node selector history-stack))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Trackable
  (track [x]))

(defn trackable? [x]
  (extends? Trackable (type x)))

(extend-protocol Trackable
  DomNode
  (track [_] true))

(defn gen-history-tracking-visitor
  "Visitor decorator that maintains a history stack for nodes for
  which (track-node? node) is true."

  [track-node? visit-fn]
  (fn history-tracking-visitor [x w]
    (if (track-node? x)
      (visit-fn x (update-in-state w :history #(conj % x)))
      (visit-fn x w))))

(defn decorate-visitor
  "Creates a visitor decorator that "
  [predicates+transformers inspect-node? visit-fn]

  (fn [node walker]
    (visit-fn
     (or (and (inspect-node? node)
              (let [history (:history walker)]
                (some (fn [[pred? transformer]]
                        (if (pred? history) (transformer node)))
                      predicates+transformers)))
         node)
     walker)))

(defn gen-decorator
  "Creates visitor function decorator that will apply the provided
  transformers to any node that matches its corresponding selector.

  Example:
   (def add-class-to-p (gen-decorator
    [[:div.interesting :p] #(dalap.html/add-class % \"bold\")])
   (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+transformers & [paired?]]

  (let [preds+transformers
        (for [[sel trn] (if paired?
                          selectors+transformers
                          (partition 2 selectors+transformers))]
          [(selector-to-predicate sel) trn])]
    (fn [visit-fn]
      (gen-history-tracking-visitor
       trackable?
       (decorate-visitor preds+transformers trackable? visit-fn)))))
