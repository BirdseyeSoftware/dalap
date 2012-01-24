(ns dalap.html.selector
  (:use [clojure.core.match
         :only
         (match)])

  (:use [dalap.defaults :only (trackable?)])
  (:use [dalap.walk :only (update-in-state)])
  (:require [dalap.html :as html]))

(defn span [p xs]
  ((juxt (partial take-while p)
         (partial drop-while p)) xs))

(defn dom-matches-selector? [dom-node selector]
  (let [[_ tag id class] (re-matches html/re-tag (name selector))]
  (and (html/has-tag-name? dom-node tag)
       (if (nil? id)
         true
         (html/has-id? dom-node id))
       (if (nil? class)
         true
         (html/has-class? dom-node class)))))

(defn- history-span [selector elem]
 (cond
   (html/dom-node? elem)
     (not (dom-matches-selector? elem selector))
   :else
     (not (= (symbol (.getName (type elem))) selector))))

(defn track-selector [selector history]
  (let [[new-history [elem & _]]
          (span #(history-span selector %) history)]
    (cond
      (nil? elem) [nil history]
      :else
        [elem new-history])))

(defn select-element [selectors history]
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
  ([actions] (partition 2 (flatten-selectors () actions)))
  ([base actions]
    (match [actions]
      [([(selector :when vector?) & sub-actions] :seq)]
        (mapcat #(flatten-selectors (vec (concat base selector)) %)
                sub-actions)
      [_]
        (list base actions))))

(defn track-visitor
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

(defn selector-visitor [selectors select-visit? visit-fn]
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

(defmacro defselector [fn-name & selectors]
  `(defn ~fn-name [visit-fn#]
    (->> (selector-visitor '~(mapcat dalap.html.selector/flatten-selectors
                                     selectors)
                           dalap.defaults/trackable?
                           visit-fn#)
         (track-visitor dalap.defaults/trackable?))))
