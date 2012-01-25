(ns dalap.walk
  "A `Walker` is a wrapper around a visitor function (a multimethod or
  a single function from a Protocol). It simplifies the recursive
  invocation of the visitor over a tree of objects and makes it
  possible to decorate or replace the visitor for sub-regions of the
  tree.
  "

  (:import [clojure.lang IFn ILookup]))

(defprotocol IWalkerState
  (get-state [this])
  (update-state [this update-fn])
  (update-in-state [this keys fn])
  (conj-state [this new-state]))


(deftype Walker [visitor state-map]

  IFn
  (invoke [this x] (visitor x this))
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))


  ;; this is main for unit-testing of the state handling
  Object
  (equals [this other]
    (and (instance? Walker other)
         (= visitor (.visitor other))
         (= state-map (.state-map other))))

  ;; the remaining methods are state-management
  IWalkerState
  (get-state [this] state-map)
  (conj-state [this new-state]
    (Walker. visitor (conj state-map new-state)))
  (update-state [this update-fn]
    (Walker. visitor (update-fn state-map)))
  (update-in-state [this ks fn]
    (let [keys (if (sequential? ks) ks [ks])]
      (Walker. visitor (update-in state-map keys fn))))
  ILookup                         ; for simpler lookup of state keys
  (valAt [this key] (state-map key))
  (valAt [this key not-found] (state-map key not-found)))


(defn gen-walker
  "A constructor wrapper around the Walker type."
  ([visitor] (gen-walker visitor {}))
  ([visitor state-map] (Walker. visitor state-map)))

(defn walk
  ([x visitor] (walk x visitor {}))
  ([x visitor state-map] ((gen-walker visitor state-map) x)))


;;;
(defn compose-visitors
  "Creates a new visitor that calls outer-visitor on the result of
  a call to inner-visitor."
  [inner-visitor outer-visitor]
  (fn comp-visitor [node walker]
    (outer-visitor (inner-visitor node walker) walker)))

(defn wrap-walker
  [visitor wrap-walker-fn]
  (fn wrapper [node walker]
    (visitor node (wrap-walker-fn node walker))))

;;;
(defn indent
  "This is an example of state manip functions that can used with the
  Walker's state-map. Call (::indent w) inside a visitor to get the
  current indent level and do some pretty printing with it."

  [w]
  (update-in-state w ::indent #(inc (or % 0))))
