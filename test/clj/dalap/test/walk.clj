^{:cljs
  (ns dalap.test.walk
    (:require [buster-cljs.core :refer [is]]
              [dalap.walk :refer
               (IWalkerState
                Walker
                get-state
                conj-state
                update-state
                update-in-state
                -gen-walker
                walk)])
    (:require-macros [buster-cljs.macros :refer [initialize-buster deftest it]]))}
(ns dalap.test.walk
  (:require [clojure.test :refer :all]
            [buster-cljs.clojure :refer [it]]
            [dalap.walk :refer
              (IWalkerState
               get-state
               conj-state
               update-state
               update-in-state
               -gen-walker
               walk)])
  (:import [dalap.walk Walker]))

#_(:cljs (initialize-buster))

(defn walker-equals [w other]
  (is (and (instance? Walker other)
           (= (.-visitor w) (.-visitor other))
           (= (.-state-map w) (.-state-map other)))))

(def identity-visitor (fn [x _] x))

(deftest test-walker-constructors
  (it "various walker constructors work identically"
   (doseq [w [(Walker. identity-visitor {})
             (-gen-walker identity-visitor)
             (-gen-walker identity-visitor {})]]
    (is (= 1 (w 1)) "1")
    (is (= (get-state w) {}) "2")
    (is (satisfies? IWalkerState w) "3")
    (is (= (.-visitor w) identity-visitor) "4"))))

(deftest test-walker-state-management
  (it "manages state correctly"
   (doseq [st [{} {:a 1234} {:a 1234 :b 99}]]
    (let [w (-gen-walker identity-visitor st)]
      (is (= (get-state w) st))
      (let [w2 (conj-state w {:c 12})]
        (is (:c w2) 12)
        (not= w w2)
        (walker-equals (update-state w2 #(dissoc % :c)) w)
        (walker-equals (update-in-state w :c #(do % 12)) w2)
        (walker-equals (conj-state w2 {:d 1 :e 2})
                       (update-state w2 #(conj % {:d 1 :e 2}))))))))


(defn recursive-visitor [n w]
  (if (vector? n)
    (into [] (map w n))
    n))

(deftest test-walker-with-recursive-visitor-fn
  (it "recur over the walk function"
    (is (= (walk [[1 2 3] [4 5]] recursive-visitor)
           [[1 2 3] [4 5]]))))
