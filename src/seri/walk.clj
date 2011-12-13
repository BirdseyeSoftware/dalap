(ns seri.walk)
(defprotocol IWalker
  (new-state [this st])
  (update-state [this keys fn])
  (get-state [this]))

(deftype Walker
    #_"A wrapper around a visitor multimethod/Protocol function. It
   facilitates the recursive invocation of `visitor-fn` by providing
   a single arity interface around it. The use of Walker also keeps
   the signature of `visitor` simple as it removes the need to define
   both single and dual arity versions of `serialize` for each
   visitable type."

    [visitor-fn state]  
  
    clojure.lang.IFn
    (invoke [this x] (visitor-fn x this))
    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))

    IWalker
    (new-state [this st] (Walker. visitor-fn st))
    (update-state [this ks fn]
      (let [keys (if (sequential? ks) ks [ks])]
        (new-state this (update-in (.state this) keys fn))))
    (get-state [this] state))

(defn walk [x visitor] ((Walker. visitor {}) x))

(defn indent [w]
  (update-state w ::indent #(inc (or % 0))))

;; (defn walk
;;   [x visitor]  
;;   ((fn walker [i] (visitor i walker)) x))
