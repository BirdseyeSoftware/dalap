(ns seri.escape)

(deftype PreEscaped [val]
  Object
  (toString [_] (str val)))

(defn safe [& xs] (PreEscaped. (apply str xs)))

(defn gen-str-escaper
  "Transforms a function that escapes a single string into one that
  will accept multimple args or a seq of strings.

  (gen-str-escaper #(. %1 (toString))) yields the equivalent of
  `clojure.core.str`"
  [normalize-single-string]
  (fn ^String str**
    (^String [] "")
    (^String [^Object x] (normalize-single-string x))
    (^String [x & ys]
             ((fn [^StringBuilder sb more]
                (if more
                  (recur (. sb (append (str** (first more)))) (next more))
                  (str sb)))
              (new StringBuilder (str** x)) ys))))

