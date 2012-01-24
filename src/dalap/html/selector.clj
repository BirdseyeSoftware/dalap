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

(defn track-selector
  "Grabs an element from history that matches the given
  selector."
  [selector history]
  (let [[new-history [elem & _]]
        (span #(history-span selector %) history)]
    (cond
      (nil? elem) [nil history]
      :else
      [elem new-history])))

(defn select-element
  "Grabas an element from history that matches a group
  of selectors, respecting a heriarchy of multiple elements
  in between."
  [selectors history]
  (loop [current-history  history
         current-selector (first selectors)
         rest-selectors   (rest selectors)]
    (let [[elem new-history]
          (track-selector current-selector current-history)]
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

(defn flatten-selectors
  "Transforms a group of selectors as received from
  defselector to a group of pairs [selector action],
  removing nested selectors and putting all of them
  on the first level."
  ([actions] (partition 2 (flatten-selectors () actions)))
  ([base actions]
     (match [actions]
       [([(selector :when vector?) & sub-actions] :seq)]
       (mapcat #(flatten-selectors (vec (concat base selector)) %)
               sub-actions)
       [_]
       (list base actions))))

(defn track-visitor
  "Visitor middleware that will track an element on the history
  of the dalap walking when the given track-visit? function returns
  true."
  [track-visit? visit-fn]
  (fn visit-decorator [x w]
    (if (track-visit? x)
      (visit-fn x (update-in-state w :history #(conj % x)))
      (visit-fn x w))))

(defn- action-to-fn [action]
  (cond
    (fn? action) action
    :else (recur (eval action))))

(defn- matching-element [selector history]
  (first (select-element selector history)))

(defn selector-visitor
  "Visitor middleware that will track all the elements on
  the dalap walk tree that matches the given selectors."
  [selectors select-visit? visit-fn]
  (fn [elem w]
    (cond
      (not (select-visit? elem))
      (visit-fn elem w)
      :else
      (for [[selector action] selectors]
        (let [matched-elem (matching-element selector (:history w))
              action-fn    (action-to-fn action)]
          (if (nil? matched-elem)
            (visit-fn nil w)
            (visit-fn (action-fn matched-elem) w)))))))

(defmacro defselector
  "Macro that will create a new visitor function that keeps track
  of elements on the dalap walk that match the given selectors.

  Example:
  > (defselector interesting-p
  >  ([:div.interesting :p] #(dalap.html/add-class % \"bold\")))
  >
  > (dalap.html/to-html html-content (interesting-p dalap.html/visit))"
  [fnname & selectors]
  `(defn ~fnname
     ([] (~fnname dalap.html/visit))
     ([visit-fn#]
        (~track-visitor
         ~trackable?
         (~selector-visitor
          '~(mapcat flatten-selectors selectors)
          ~trackable? visit-fn#)))))
