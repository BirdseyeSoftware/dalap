(ns dalap.walk
  ;; ^{:doc "A `Walker` is a wrapper around a visitor function (a
  ;; multimethod or a single function from a Protocol). It simplifies the
  ;; recursive invocation of the visitor over a seq of objects (possibly
  ;; nested) and makes it possible to decorate or replace the visitor for
  ;; sub-regions of the seq."}
  ^:clj (:import [clojure.lang IFn ILookup]))

(defprotocol IWalkerState
  "IWalkerState adds a state interface to Walker. Initially this
  protocol is being used only on dalap.walk/Walker; if you need to
  implement your own walker and you want to hold a state on this
  walker, you should use this protocol to do so."

  (get-state [this]
     "Returns the state of a walker")
  (update-state [this update-fn]
     "Updates the state of a walker using `update-fn`")
  (update-in-state [this keys fn]
     "Updates a value in the state map pointed by `keys`, it
      uses the `fn` function to transform the value.")
  (conj-state [this new-state]
     "Conj's a value into the state map."))


(deftype Walker [visitor state-map]
  ;; A walker is a wrapper around a `visitor` function and an optional
  ;; state-map.  When a walker is called as a function it dispatches
  ;; to the `visitor` function:
  ;;
  ;; (walker-instance node) === (visitor node walker-instance)
  ;;
  ;; The optional `state-map` can be used by visitors to implement
  ;; stateful traversal behaviours, such as an indenting
  ;; pretty-printer or css-like parent/child selectors. Use it like
  ;; you would use the State Monad.

  IFn
  (^{:cljs -invoke} invoke
   [this x] (visitor x this))
  ; ^ Calling a Walker as a function will call the internal
  ; `visitor` function.

  ^:clj
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))
  ; ^ Make it easy to use the apply function with the walker instance

  IWalkerState
  ; This code should be self explinatory; for all the update functions
  ; we create a new Walker instance to keep the records persistent
  (get-state [this] state-map)
  (conj-state [this new-state]
    (Walker. visitor (conj state-map new-state)))
  (update-state [this update-fn]
    (Walker. visitor (update-fn state-map)))
  (update-in-state [this ks fn]
    (let [keys (if (sequential? ks) ks [ks])]
      (Walker. visitor (update-in state-map keys fn))))

  ILookup
  ; Allow the walker to be queried for state keys as if it were a map.
  (^{:cljs -lookup} valAt [this key] (state-map key))
  (^{:cljs -lookup} valAt [this key not-found] (state-map key not-found)))

(defn -gen-walker
  "Builds a `dalap.walk/Walker` that wraps the provide visitor function."
  ([visitor] (-gen-walker visitor {}))
  ([visitor state-map] (Walker. visitor state-map)))

(defn walk
  "Builds a `dalap.walk/Walker` that wraps the provided visitor
  function, and then calls the walker on the given input object."
  ([input visitor] (walk input visitor {}))
  ([input visitor state-map] ((-gen-walker visitor state-map) input)))
