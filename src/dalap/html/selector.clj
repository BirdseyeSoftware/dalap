(ns dalap.html.selector
  (:use [clojure.core.match
         :only
         (match)])

  (:use [dalap.walk :only (update-in-state)])
  (:require [dalap.html :as html])
  (:import [dalap.html DomNode]))

(defprotocol Trackable
  (track [x]))

(defn trackable? [x]
  (extends? Trackable (type x)))

(extend-protocol Trackable
  DomNode
  (track [_] true))

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

(defn- history-span
  "Does an span using dom-matches-selector? on elements
  that are DomNode type, and compares types otherwise."
  [selector elem]
  (cond
    (html/dom-node? elem)
    (not (dom-matches-selector? elem selector))
    :else
    (not (= (symbol (.getName (type elem))) selector))))

(defn match-selector
  "Grabs an element from history that matches the given
  selector."
  [selector history]
  (let [[new-history [elem & _]]
        (span #(history-span selector %) history)]
    (cond
      (nil? elem) [nil history]
      :else
      [elem new-history])))

(defn match-selector*
  "Multiple selector version of match-selector.

  Grabs an element from history that matches a group
  of selectors, respecting a heriarchy of multiple elements
  in between."
  [selectors history]
  (loop [current-history  history
         current-selector (first selectors)
         rest-selectors   (rest selectors)]
    (let [[elem new-history]
          (match-selector current-selector current-history)]
      (cond
        (nil? elem) [nil history]

        (and (not (nil? elem))
             (empty? rest-selectors)
             (not (empty? new-history))) [nil history]

             (and (not (nil? elem))
                  (empty? rest-selectors)) [elem history]

                  :else
                  (recur new-history
                         (first rest-selectors)
                         (rest rest-selectors))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gen-history-tracking-visitor
  "Visitor middleware that will track an element on the history
  of the dalap walking when the given track-visit? function returns
  true."
  [track-visit? visit-fn]
  (fn history-tracking-visitor [x w]
    (if (track-visit? x)
      (visit-fn x (update-in-state w :history #(conj % x)))
      (visit-fn x w))))

(defn- action-to-fn [action]
  ;; evil this needs removing
  (cond
    (fn? action) action
    :else (recur (eval action))))

(defn- matching-element [selector history]
  (first (match-selector* selector history)))

(defn decorate-visitor
  "Visitor middleware that will track all the elements on
  the dalap walk tree that matches the given selectors."
  [selectors+actions select-visit? visit-fn]
  (fn [elem w]
    (cond
      (not (select-visit? elem))
      (visit-fn elem w)
      :else
      (let [history (:history w)]
        (or (some (fn [[selector action]]
                    (if-let [matched-elem (matching-element
                                           selector history)]
                      (visit-fn ((action-to-fn action)
                                 matched-elem)
                                w)))
                  selectors+actions)
            (visit-fn elem w))))))

(defn gen-decorator [selectors+actions & [paired?]]
  (fn [visitor-fn]
    (gen-history-tracking-visitor
     trackable?
     (decorate-visitor
      (if paired?
        selectors+actions
        (partition 2 selectors+actions))
      trackable? visitor-fn))))

(defn flatten-selectors
  "Transforms a seq of selectors+actions as received from
  defdecorator to a seq of pairs [selector action],
  removing nested selectors and putting all of them
  on the first level."
  ([selectors+actions]
     (partition 2 (flatten-selectors () selectors+actions)))
  ([base selectors+actions]
     (match [selectors+actions]
       [([(selector :when vector?) & sub-actions] :seq)]
       (mapcat #(flatten-selectors (vec (concat base selector)) %)
               sub-actions)
       [_]
       ;; I think this is a bug with selectors like [:div]
       (list base selectors+actions))))

(defmacro defdecorator
  "Macro that will create a new visitor function that keeps track
  of elements on the dalap walk that match the given selectors.

  Example:
  > (defdecorator interesting-p
  >  ([:div.interesting :p] #(dalap.html/add-class % \"bold\")))
  >
  > (dalap.html/to-html html-content (interesting-p dalap.html/visit))"
  [fnname & selectors]
  `(defn ~fnname
     ([] (~fnname dalap.html/visit))
     ([visit-fn#]
        (~gen-history-tracking-visitor
         ~trackable?
         (~decorate-visitor
          '~(mapcat flatten-selectors selectors)
          ~trackable? visit-fn#)))))
