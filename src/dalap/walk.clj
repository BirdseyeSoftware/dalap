(ns dalap.walk
  ^{ :doc "A `Walker` is a wrapper around a visitor function (a
  multimethod or a single function from a Protocol). It simplifies the
  recursive invocation of the visitor over a seq of objects (possibly
  nested) and makes it possible to decorate or replace the visitor for
  sub-regions of the seq."})

(defprotocol IWalkerState
  "API: Public

  The IWalkerState is used to add a state interface to a
  Walker type. Initially this protocol is being used only on
  dalap.walk/Walker; if you need to implement your own walker
  and you want to hold a state on this walker, you should use this
  protocol to do so."

  (get-state [this]
     "Returns the state of a walker")
  (update-state [this update-fn]
     "Updates the state of a walker using `update-fn`")
  (update-in-state [this keys fn]
     "Updates a value in the state map pointed by `keys`, it
      uses the `fn` function to transform the value.")
  (conj-state [this new-state]
     "Conjs a value into the state map."))


(deftype Walker [visitor state-map]
  ;; API: Internal
  ;;
  ;; A walker is basicaly a function that holds an internal
  ;; state that can be transformed, and a visitor function.
  ;;
  ;; To create a walker you need to provide a "visitor" function,
  ;; this visitor function will be called each time you call the
  ;; walker with a parameter; this is what happens under the sheets:
  ;;
  ;; (walker-instance node) === (visitor node walker-instance)
  ;;
  ;; The walker state serves to keep a hold on data while you are
  ;; iterating over a structure. Walkers purpose is to call itself
  ;; recursively in data structures, to map one object into another.

  clojure.lang.IFn
  (invoke [this x] (visitor x this))
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

  clojure.lang.ILookup
  ; We want to keep the behavior given on defrecord, that record
  ; attributes can be accessed using symbol functions.
  (valAt [this key] (state-map key))
  (valAt [this key not-found] (state-map key not-found)))

(defn -gen-walker
  "Signature: (([^Object ^dalap.walk/Walker] -> Output) -> dalap.walk/Walker)

  Builds a `dalap.walk/Walker` instance using the provided visitor
  function.  If a state-map is provided it would use it as the walker
  state-map, otherwise an empty map is used."
  ([visitor] (-gen-walker visitor {}))
  ([visitor state-map] (Walker. visitor state-map)))

(defn walk
  "Builds a `dalap.walk/Walker` instance using the provided visitor
  function, and then runs the walker on the given input object. If a
  starte-map is given, it would use it as the internal state, otherwise an
  empty PersistentMap is used."
  ([input visitor] (walk input visitor {}))
  ([input visitor state-map] ((-gen-walker visitor state-map) input)))


;;;

;; (defn indent
;;   "Signature: [^dalap.walk/IWalkerState] -> dalap.walk/IWalkerState

;;   API: Public

;;   This is an example of functions that use the Walker's state-map.
;;   Call `(:dalap.walk/indent walker-instance)` inside a visitor to
;;   get the current indent level and do some pretty printing with it."
;;   [walker]
;;   (update-in-state walker ::indent #(inc (or % 0))))
