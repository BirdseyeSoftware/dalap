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
      (dom-matches-selector? node single-selector)
      :else
      (= (type node) single-selector)
      )))

(defn match-selector
  "Grabs a node from history that matches the given
  selector."
  [selector history]
  (let [match-node? (gen-matcher-pred selector)
        [new-history [node & _]] (span (complement  #(match-node? %)) history)]
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

(defn- gen-visitor-from-pred-trans-pairs
  [predicates+transformers inspect-node?]
  (fn [node walker]
    (or (and (inspect-node? node)
             (let [history-stack (:history walker)]
               (some (fn [[pred? transformer]]
                     (if (pred? history-stack)
                       (transformer node)))
                   predicates+transformers)))
        node)))

(defn gen-decorator
  "Creates a visitor function decorator that will apply the provided
  transformers to any node that matches its corresponding selector.

  Example:
   (def add-class-to-p (gen-decorator
    [[:div.interesting :p] #(dalap.html/add-class % \"bold\")])
   (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+transformers & [paired?]]

  (let [inspect-node? identity ;; track everything except nil/false
        preds+transformers
        (for [[sel trn] (if paired?
                          selectors+transformers
                          (partition 2 selectors+transformers))]
          [(selector-to-predicate sel) trn])
        inner-visitor (gen-visitor-from-pred-trans-pairs
                       preds+transformers inspect-node?)
        add-history-to-walker (fn [node w]
                                (if (inspect-node? node)
                                  (update-in-state w :history #(conj % node))
                                  w))]
    (fn [visit-fn]
      (wrap-walker (compose-visitors inner-visitor visit-fn)
                   add-history-to-walker))))
