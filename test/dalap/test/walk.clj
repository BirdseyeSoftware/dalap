(ns dalap.test.walk
  (:use [clojure.test])

  (:use [dalap.walk :only
         [IWalkerState get-state conj-state update-state update-in-state
          gen-walker walk]])
  (:import [dalap.walk Walker]))

(def identity-visitor (fn [x _] x))

(defn test-ident-walker [w]
  (is (= 1 (w 1)))
  (is (= (get-state w) {}))
  (is (satisfies? IWalkerState w))
  (is (= (.visitor w) identity-visitor)))

(deftest test-constructors
  (doseq [w [(Walker. identity-visitor {})
             (gen-walker identity-visitor)
             (gen-walker identity-visitor {})]]
    (test-ident-walker w)))

(deftest test-state-management
  (doseq [st [{} {:a 1234} {:a 1234 :b 99}]]
    (let [w (gen-walker identity-visitor st)]
      (is (= (get-state w) st))
      (let [w2 (conj-state w {:c 12})]
        (is (:c w2) 12)
        (not= w w2)
        (= (update-state w2 #(dissoc % :c)) w)
        (= (update-in-state w :c #(do % 12)) w2)
        (= (conj-state w2 {:d 1 :e 2})
           (update-state w2 #(conj % {:d 1 :e 2})))))))
