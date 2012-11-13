;; This file was generated with dalap-cljsbuild from
;;
;; src/clj/dalap/selector.clj @ Tue Nov 13 18:28:52 UTC 2012
;;
(ns dalap.selector (:require [dalap.walk :refer (update-in-state)]))
(defn- span [p xs] ((juxt (partial take-while p) (partial drop-while p)) xs))
(defn -compose-visitors "Creates a visitor function that passes it's parameter to the\n  given inner-visitor function, then the result of this call is going\n  to be passed to the outer-visitor function, using the same walker\n  on both calls." [inner-visitor outer-visitor] (fn comp-visitor [input walker] (outer-visitor (inner-visitor input walker) walker)))
(defn -wrap-walker "Modifies the walker instance when navigating through the input, you\n  would like to use this function when you want to transform the walker\n  somehow while you are visiting an element of the input. This is intended\n  to be called from a visitor function.\n  " [visitor -wrap-walker-fn] (fn wrapped-visitor [input walker] (visitor input (-wrap-walker-fn input walker))))
(defprotocol IRuleSelector "Provides `to-rule-selector`, which converts a selector `selectable`\n  into a matcher predicate that matches locations in a dalap input\n  tree. It can match on the type or attributes of the current node or,\n  optionally, also match on the parents of the current\n  node.\n\n  The signature of the generated matcher function is [node, walker],\n  which is the same signature as dalap visitors. Any non-nil /\n  non-false return value is considered a match." (to-rule-selector [selectable]))
(defn -match-selector "Grabs a single node from history that matches the given selector." [selector? history] (let [[new-history [node & _]] (span (fn selector-span [n] (not (selector? n nil))) history)] (if (nil? node) [nil history] [node new-history])))
(defn -match-selector* "Multiple selector version of -match-selector.\n\n  Grabs a node from history that matches a group of selectors,\n  respecting a heriarchy of multiple nodes in between." [selectors history] (loop [current-history history current-selector (first selectors) rest-selectors (rest selectors)] (let [[node new-history] (-match-selector current-selector current-history)] (cond (nil? node) [nil history] (and (empty? rest-selectors) (not (empty? new-history))) [nil history] (empty? rest-selectors) [node history] :else (recur new-history (first rest-selectors) (rest rest-selectors))))))
(defn- -matching-node [selector history] (first (-match-selector* selector history)))
(extend-protocol IRuleSelector PersistentVector (to-rule-selector [selector-vec] (let [selector (map to-rule-selector selector-vec)] (fn location-matcher [_node walker] (let [history-stack (:history walker)] (-matching-node selector history-stack))))) string (to-rule-selector [s] (cond (symbol? s) (fn symbol-matcher [node _walker] (= node s)) :else (throw (js/Error. (str "No tree-loc-matcher for " (type s)))))) function (to-rule-selector [sfn] sfn))
(defprotocol IRuleTransformer "Adapts other types to dalap visitor function." (to-rule-transformer [adaptable] "Creates a dalap visitor."))
(extend-protocol IRuleTransformer cljs.core.PersistentHashMap (to-rule-transformer [m] (fn map-visitor [node _w] (m node))))
(defn- ifn-to-rule-transformer [vfn] (fn single-arg-fn-visitor [node _w] (vfn node)))
(extend-protocol IRuleTransformer function (to-rule-transformer [vfn] (ifn-to-rule-transformer vfn)))
(extend-protocol IRuleTransformer PersistentVector (to-rule-transformer [v] (fn vec-transformer-visitor [_node _walker] v)) string (to-rule-transformer [s] (cond (symbol? s) (fn symbol-transformer-visitor [_node _walker] s) (keyword? s) (fn keyword-transformer-visitor [node _walker] (s node)) :else (fn string-transformer-visitor [node _walker] s))) default (to-rule-transformer [obj] (fn object-transformer-visitor [_node _walker] obj)))
(defn -get-transformer-of-first-matching-rule [node walker rules] (letfn [(transformer-if-match [[selector? transformer]] (if (selector? node walker) transformer))] (some transformer-if-match rules)))
(defn- -gen-visitor-from-rules [rules inspect-node-fn?] (fn rules-visitor [node walker] (if (inspect-node-fn? node) (let [visitor (or (-get-transformer-of-first-matching-rule node walker rules) (constantly node))] (visitor node walker)) node)))
(defn normalize-rules [rules] (for [[selector transformer] rules] [(to-rule-selector selector) (to-rule-transformer transformer)]))
(defn -gen-rules-decorator "Revisit documentation" [rules] (let [inspect-node? identity rule-pairs (partition 2 rules) inner-visitor (-gen-visitor-from-rules (normalize-rules rule-pairs) inspect-node?) add-history-to-walker (fn add-hist [node w] (if (inspect-node? node) (update-in-state w :history (fn* [p1__5455#] (conj p1__5455# node))) w))] (fn rules-decorator [visit-fn] (-wrap-walker (-compose-visitors inner-visitor visit-fn) add-history-to-walker))))
(defn gen-rules-visitor "API: Public" ([rules fallback-visitor] ((-gen-rules-decorator rules) fallback-visitor)))