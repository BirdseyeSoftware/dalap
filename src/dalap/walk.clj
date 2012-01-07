(ns dalap.walk
  "A `Walker` is a wrapper around a visitor function (a multimethod or
  a single function from a Protocol). It simplifies the recursive
  invocation of the visitor over a tree of objects and makes it
  possible to decorate or replace the visitor for sub-regions of the
  tree.
  "

  (:import [clojure.lang IFn ILookup]))

(defprotocol IWalker
  (get-visitor-fn [this])
  (update-in-state [this keys fn])
  (get-state [this]))

(deftype Walker
    [visitor state-map]

    IFn
    (invoke [this x] (visitor x this))
    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))

    IWalker
    (get-visitor-fn [this] visitor)
    (update-in-state [this ks fn]
      (let [keys (if (sequential? ks) ks [ks])]
        (Walker. visitor (update-in state-map keys fn))))
    (get-state [this] state-map)

    ILookup
    (valAt [this key] (state-map key))
    (valAt [this key not-found] (state-map key not-found)))

(defn gen-walker
  "A constructor wrapper around the Walker type."
  ([visitor] (gen-walker visitor {}))
  ([visitor state-map] (Walker. visitor state-map)))

(defn walk
  ([x visitor] (walk x visitor {}))
  ([x visitor state-map] ((gen-walker visitor state-map) x)))

(defn indent
  "This is an example of state manip functions that can used with the
  Walker's state-map. Call (::indent w) inside a visitor to get the
  current indent level and do some pretty printing with it."

  [w]
  (update-in-state w ::indent #(inc (or % 0))))
