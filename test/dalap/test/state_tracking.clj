(ns dalap.test.state_tracking
  (:require [clojure.test :refer :all]
            [clojure.core.match :as match])
  (:import [clojure.lang Named ISeq Seqable])

  (:require [dalap.html :as html]
            [dalap.defaults]
            [dalap.walk :refer
              (IWalkerState
               get-state
               conj-state
               update-state
               update-in-state
               -gen-walker
               walk)])
  (:import [dalap.html DomNode]
           [dalap.walk Walker]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn dom-node? [e]
;  (instance? DomNode e))
;
;(defn trackable? [e]
;  (extends? dalap.defaults/Trackable (type e)))
;
;(defn tag-with-name? [tag-name]
;  (fn [e]
;    (and (dom-node? e)
;         (= (-> e :tag name)
;            (name tag-name)))))
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


; history is in a monadic state
;
; (select [:div.hello :p.pre] (add-class "other"))
;
; ; calls internally
;
; (get-element-from-selector [:div.hello :p.pre])
;
; => (domonad (maybe-t state-m)
;      [_      (track-or-fail :div.hello)
;       result (track-or-fail :p.pre)]
;       result)
;
; (defn dom-matches-selector? [dom-node selector]
;   (let [_ tag id class] (re-matches re-tag (name selector))
;   (tag-name? dom-node tag)))
;
; (defn track-on-history [selector]
;   (domonad (maybe-t state-m)
;     [history (fetch-state)
;      :let [[_ [node & new-history] (span #(dom-matches-selector? % selector)
;                                          history)]
;      _ (set-state new-history)]
;      node))
;
;
; (defselector visitor-fn
;   ([:div.container]
;
;     ([:p.pre]
;       (insert-before
;         [:div.something
;           [:p "hello world"]]))
;
;     (insert-after
;       ["hello world"]))
;
;   ([:body.hello]
;     (add-class "hello")))
;
;
;(defn select [selector])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn track-visit
;  [track-visit? visit-fn]
;  (fn visit-decorator [x w]
;    (if (track-visit? x (::history w))
;      (visit-fn x (update-in-state w ::history #(conj % x)))
;      (visit-fn x w))))
;
;(defn p-bold-class [visit-fn]
;  (fn visit-decorator [x w]
;    (if ((tag-with-name? "p") x)
;      (visit-fn (html/add-class x "bold") w)
;      (visit-fn x w))))
;
;(println
;  (html/to-html [:div.hello [
;                        [:p.first "Roman"]
;                        [:p.last  "Gonzalez"]
;                      ]]
;                      (track-visit (fn [x _] (trackable? x))
;                        (p-bold-class html/visit))))
;
;(defprotocol Visitable
;  (visit [o w]))
;
;(extend-protocol Visitable
;  Object
;  (visit [o w] (df/visit o w)))
;
;(defrecord InnerWrap [children]
;  Visitable
;  (visit [_ w]
;    (map (comp w #(vector :div %)) children)))
;
;(defn wrap-map [wrapper walker container]
;  (map (comp walker
;             (wrapper e))
;       container))
;
;(deftype OuterWrap [children]
;  Visitable
;  (visit [o w]
;    (wrap-map ->InnerWrap w children)))
;
;(def inner-wrapper? #(instance? InnerWrap %))
;(defn state-tracking-visitor
;  [x w0]
;  (let [w (update-in-state w0
;                           ::visited #(conj % x))
;        history (::visited w)]
;    (match/match [x history]
;      [(k :when keyword?) _]
;      [k " "]
;
;      [_ ([_current (_parent :when inner-wrapper?) & r] :seq)]
;      (visit ["*" x " "] w)
;
;      [_ _] (visit x w))))
;
;(deftest test-state
;  (is (= "*:div a *:div 1 *:div foo *:div 0123456789 *:div b "
;         (apply str
;                (flatten
;                 (walk (OuterWrap. [ "a" [1 'foo (range 10)] "b"])
;                       state-tracking-visitor))))))
