(ns seri.walk)
(defprotocol IWalker
  (update-in-state [this keys fn])
  (get-state [this]))

(deftype Walker
    #_"A wrapper around a visitor multimethod/Protocol function. It
   facilitates the recursive invocation of `visitor-fn` by providing
   a single arity interface around it. The use of Walker also keeps
   the signature of `visitor` simple as it removes the need to define
   both single and dual arity versions of `serialize` for each
   visitable type."

    [visitor state-map]  
  
    clojure.lang.IFn
    (invoke [this x] (visitor x this))
    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))

    IWalker
    (update-in-state [this ks fn]
      (let [keys (if (sequential? ks) ks [ks])]
        (Walker. visitor (update-in state-map keys fn))))
    (get-state [this] state-map)

    clojure.lang.ILookup
    (valAt [this key] (state-map key))
    (valAt [this key not-found] (state-map key not-found)))

(defn walk [x visitor] ((Walker. visitor {}) x))

(defn indent
  "This is an example of state manip functions that can used with the
  Walker's state-map. Call (::indent w) inside a visitor to get the
  current indent level and do some pretty printing with it."

  [w]
  (update-in-state w ::indent #(inc (or % 0))))

;; (defn walk
;;   [x visitor]  
;;   ((fn walker [i] (visitor i walker)) x))
