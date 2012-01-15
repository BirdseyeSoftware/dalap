(ns dalap.test.state_tracking
  (:use [clojure.test])
  (:require [clojure.core.match :as match])

  (:use [dalap.walk :only
         [IWalkerState get-state conj-state update-state update-in-state
          gen-walker walk]])
  (:require [dalap.defaults :as df])
  (:import [clojure.lang Named ISeq Seqable])
  (:import [dalap.walk Walker]))


(defprotocol Visitable
  (visit [o w]))

(extend-protocol Visitable
  Object
  (visit [o w] (df/visit o w)))

(defrecord InnerWrap [children]
  Visitable
  (visit [o w]
    (map (comp w #(vector :div %)) children)))

(defn wrap-map [wrapper walker container]
  (map (comp walker
             (fn [e] (with-meta (wrapper e) {::dyn-wrapper true})))
       container))

(deftype OuterWrap [children]
  Visitable
  (visit [o w]
    (wrap-map ->InnerWrap w children)))

(def inner-wrapper? #(instance? InnerWrap %))
(defn state-tracking-visitor
  [x w0]
  (let [w (update-in-state w0
                           ::visited #(conj % x))
        history (::visited w)]
    (match/match [x history]
      [(k :when keyword?) _]
      [k " "]

      [_ ([_current (_parent :when inner-wrapper?) & r] :seq)]
      (visit ["*" x " "] w)

      [_ _] (visit x w))))

(deftest test-state
  (is (= "*:div a *:div 1 *:div foo *:div 0123456789 *:div b "
         (apply str
                (flatten
                 (walk (OuterWrap. [ "a" [1 'foo (range 10)] "b"])
                       state-tracking-visitor))))))
