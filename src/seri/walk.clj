(ns seri.walk)
(deftype Walker

    #_"A wrapper around a visitor multimethod/Protocol function. It
    facilitates the recursive invocation of `visitor-fn` by providing
    a single arity interface around it. The use of Walker also keeps
    the signature of `visitor` simple as it removes the need to define
    both single and dual arity versions of `serialize` for each visitable
    type."

    [visitor-fn]  
    clojure.lang.IFn
    (invoke [this x] (visitor-fn x this))
    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))

(defn walk [x visitor] ((Walker. visitor) x))
