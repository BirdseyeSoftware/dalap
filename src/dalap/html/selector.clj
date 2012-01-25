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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Trackable
  (track [x]))

(defn trackable? [x]
  (extends? Trackable (type x)))

(extend-protocol Trackable
  DomNode
  (track [_] true))

(defn gen-history-tracking-visitor
  "Visitor middleware that will track an node on the history
  of the dalap walking when the given track-visit? function returns
  true."
  [track-visit? visit-fn]
  (fn history-tracking-visitor [x w]
    (if (track-visit? x)
      (visit-fn x (update-in-state w :history #(conj % x)))
      (visit-fn x w))))

(defn- matching-node [selector history]
  (first (match-selector* selector history)))

(defn decorate-visitor
  "Visitor middleware that will track all the nodes on
  the dalap walk tree that matches the given selectors."
  [selectors+actions inspect-node? visit-fn]
  (fn [node w]
    (cond
      (inspect-node? node)
      (let [history (:history w)]
        (or (some (fn [[selector action]]
                    (if-let [match (matching-node selector history)]
                      (visit-fn (action match) w)))
                  selectors+actions)
            (visit-fn node w)))
      :else
      (visit-fn node w))))

(defn gen-decorator
  "Creates visitor function decorator that will apply the provided
  actions to any node that matches its corresponding selector.

  Example:
   (def add-class-to-p (gen-decorator
    [[:div.interesting :p] #(dalap.html/add-class % \"bold\")])
   (dalap.html/to-html html-content (add-class-to-p dalap.html/visit))"
  [selectors+actions & [paired?]]

  (let [selectors+actions (if paired?
                            selectors+actions
                            (partition 2 selectors+actions))]
    (fn [visit-fn]
      (gen-history-tracking-visitor
       trackable?
       (decorate-visitor selectors+actions trackable? visit-fn)))))
