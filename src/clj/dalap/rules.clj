(ns dalap.rules
  (:refer-clojure :exclude [when])
  (:require [dalap.walk :refer (update-in-state)]))

;; NOTE: all the dynamically created functions (from `fn`) in this
;; module are given names to aid in debugging
(defn- span [p xs]
  ((juxt (partial take-while p)
         (partial drop-while p)) xs))

(defn -compose-visitors
  "Creates a visitor function that passes it's parameter to the
  given inner-visitor function, then the result of this call is going
  to be passed to the outer-visitor function, using the same walker
  on both calls."
  [inner-visitor outer-visitor]
  (fn comp-visitor [input walker]
    (outer-visitor (inner-visitor input walker) walker)))

(defn -wrap-walker
  "Modifies the walker instance when navigating through the input, you
  would like to use this function when you want to transform the walker
  somehow while you are visiting an element of the input. This is intended
  to be called from a visitor function.
  ";@@TODO: revisit the wording
  [visitor -wrap-walker-fn]
  (fn wrapped-visitor [input walker]
    (visitor input (-wrap-walker-fn input walker))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IRuleSelector
  "Provides `to-rule-selector`, which converts a selector `selectable`
  into a matcher predicate that matches locations in a dalap input
  tree. It can match on the type or attributes of the current node or,
  optionally, also match on the parents of the current
  node.

  The signature of the generated matcher function is [node, walker],
  which is the same signature as dalap visitors. Any non-nil /
  non-false return value is considered a match."
  (to-rule-selector [selectable]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -match-selector
  "Grabs a single node from history that matches the given selector."
  [selector? history]
  (let [[new-history [node & _]]
        (span (fn selector-span [n]
                ;; second arg is walker that we don't have
                (not (selector? n nil)))
              history)]
    (if (nil? node)
      [nil history]
      [node new-history])))

(defn -match-selector*
  "Multiple selector version of -match-selector.

  Grabs a node from history that matches a group of selectors,
  respecting a heriarchy of multiple nodes in between."
  [selectors history]
  (loop [current-history  history
         current-selector (first selectors)
         rest-selectors   (rest selectors)]

   (let [[node new-history]
          (-match-selector current-selector current-history)]
      (cond
        ;; when current selector does not match halt
        (nil? node) [nil history]

        ;; when selectors are done and history still
        ;; going, just halt, didn't match
        (and (empty? rest-selectors)
             (not (empty? new-history)))
        [nil history]

        ;; when selectors is empty, just return node
        ;; and history *we have a match*
        (empty? rest-selectors)
        [node history]

        ;; recurse with the rest of the history until
        ;; rest of selectors to be empty
        :else
        (recur new-history
               (first rest-selectors)
               (rest rest-selectors))))))

(defn- -matching-node [selector history]
  (first (-match-selector* selector history)))

(extend-protocol IRuleSelector
  ;; For vectors, check that each element
  ;; matches in the history-stack using the NodeMatcher
  ;; impl of each given selector.
  clojure.lang.PersistentVector
  (to-rule-selector [selector-vec]
    (let [selector (map to-rule-selector selector-vec)]
      (fn location-matcher [_node walker]
        (let [history-stack (:history walker)]
          (-matching-node selector history-stack))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(:cljs
   (extend-protocol IRuleSelector
     string
     (to-rule-selector [s]
       (cond
         (symbol? s) (fn symbol-matcher [node _walker] (= node s))
         :else (throw (js/Error. (str "No IRuleSelector instance for type `"
                                      (type s) "`, value: `" s "`")))))))

^:clj
(extend-protocol IRuleSelector
  clojure.lang.Symbol
  (to-rule-selector [sym]
    (fn [node _walker] (= node sym))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

^:clj
(extend-protocol IRuleSelector
  java.lang.Class
  (to-rule-selector [kls]
    (fn class-rule-selector [node _walker] (= (type node) kls))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IRuleTransformer
  "Adapts other types to dalap visitor function."

  (to-rule-transformer [adaptable] "Creates a dalap visitor."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The IPersistentMap implementation of IRuleTransformer
;; need to be defined via extend rather than via extend-protocol.
;; For Clojure 1.4 it makes no difference for 64-bit JVMs,
;; but with 32-bit JVMs the protocol doesn't dispatch correctly and we
;; end up with arity exceptions.

^{:cljs
  (extend-protocol IRuleTransformer
    cljs.core.PersistentHashMap
    (to-rule-transformer [m] (fn map-visitor [node _w] (m node))))}
(extend clojure.lang.IPersistentMap
  IRuleTransformer
  {:to-rule-transformer (fn map-visitor-adapter [m]
                          (fn map-visitor [node _w] (m node)))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (extend-protocol IRuleTransformer
;;   ;; PersistentVectors replace the current node of the dalap tree
;;   ;; with themselves.
;;   ;;
;;   ;; This would match the IFn implementation if this more specific one
;;   ;; were not provided here.
;;   clojure.lang.PersistentVector
;;   (to-rule-transformer [v]
;;     (fn vec-transformer-visitor [_node _walker] v)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

^:clj
(extend-protocol IRuleTransformer
  ;; Keywords will be called as functions on the nodes of the dalap tree.
  clojure.lang.Keyword
  (to-rule-transformer [k]
    (fn keyword-transformer-visitor [node _walker] (k node))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(:cljs
   (extend-protocol IRuleTransformer

     string
     (to-rule-transformer
       [s]
       (cond
         (symbol? s) (fn symbol-transformer-visitor [_node _walker] s)
         ;;
         (keyword? s) (fn keyword-transformer-visitor [node _walker] (s node))
         ;;
         :else (fn string-transformer-visitor [node _walker] s)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IRuleTransformer
  ;; All other Objects replace the current node of the dalap tree
  ;; with themselves.
  ^{:cljs default}
  Object
  (to-rule-transformer [obj]
    (fn object-transformer-visitor [_node _walker] obj)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype FnRule [f]
  ;;;
  IRuleSelector
  (to-rule-selector [_] (fn [node walker_] (f node)))
  ;;;
  IRuleTransformer
  (to-rule-transformer [_] (fn [node] (f node))))

(defn when
  ""
  [f]
  (FnRule. f))

(def transform when)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -get-transformer-of-first-matching-rule [node walker rules]
  (letfn [(transformer-if-match [[selector? transformer]]
            (if (selector? node walker) transformer))]
    (some transformer-if-match rules)))

(defn- -gen-visitor-from-rules
  [rules inspect-node-fn?]
  (fn rules-visitor [node walker]
    (if (inspect-node-fn? node)
      (let [visitor (or (-get-transformer-of-first-matching-rule
                         node walker rules)
                        ;; or fall through with an identity visitor
                        (constantly node))]
        (visitor node walker))
      node)))

(defn -normalize-rules [rules]
  (for [[selector transformer] rules]
    [(to-rule-selector selector)
     (to-rule-transformer transformer)]))

(defn -gen-rules-decorator
  ""
  [rules]
  (let [inspect-node? identity
        ;; ^ track/match-on everything except nil/false
        ;; TODO: it might better to do (constantly true) rather than identity
        rule-pairs (partition 2 rules)
        inner-visitor (-gen-visitor-from-rules (-normalize-rules rule-pairs)
                                               inspect-node?)
        add-history-to-walker (fn add-hist [node w]
                                (if (inspect-node? node)
                                  (update-in-state w :history #(conj % node))
                                  w))]
    (fn rules-decorator [visit-fn]
      (-wrap-walker (-compose-visitors inner-visitor visit-fn)
                    add-history-to-walker))))


(defn gen-rules-visitor
  ""
  ([rules fallback-visitor]
    ((-gen-rules-decorator rules) fallback-visitor)))
