(ns dalap.escape)

(deftype PreEscaped [val]
  Object
  (toString [_] (str val)))

(defn ^{:api :public} safe
  "Signature: ([& xs] -> PreEscaped)
  API: Public

  Transforms one or several objects into a String and then
  wraps it into a `dalap.escape/PreEscaped` record. When using
  escaping functions on this type, they will only unwrap the value without
  doing any escaping whatsoever to the inner String."
  [& xs] (PreEscaped. (apply str xs)))

(defn ^{:api :internal} -gen-str-escaper
  "Signature: (([^Object] -> String) -> ([& xs] -> String))
  API: Internal

  Transforms a function that escapes a single string into one that
  accepts multiple args or a seq of strings.

  Example:

  (-gen-str-escaper #(. %1 toString))

  returns the equivalent of `clojure.core/str`"
  [single-string-fn]
  (fn ^String str**
    (^String [] "")
    (^String [^Object x] (single-string-fn x))
    (^String [x & ys]
             ((fn [^StringBuilder sb more]
                (if more
                  (recur (. sb (append (str** (first more)))) (next more))
                  (str sb)))
              (new StringBuilder (str** x)) ys))))
