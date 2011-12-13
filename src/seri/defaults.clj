(ns seri.defaults
  (:use [seri.walk :only [walk]])
  (:import [clojure.lang Named ISeq Seqable]))

(defprotocol Serializable
  "A protocol providing the default implementation of Seri's
  serialization visitor interface."
  (visit [x walker]))

(extend-protocol Serializable
  ;; Object impl not provided to avoid subtle/silent bugs
  nil (visit [_ _] "")
  String (visit [s _] s)
  Character (visit [c _] (str c)) 
  Number (visit [n _] (str n))
  Named (visit [o _] (str o)))

(defn visit-seq [s w] (map w s))
(extend Seqable Serializable {:visit visit-seq})

(defn serialize           
  "Walks, flattens and serializes an object (or graph of objects) into
  a String.  The default implementation does no escaping of the output."

  ([x] (serialize x visit str))
  ([x visitor] (serialize x visitor str))

  ([x visitor output-escaper]
     (apply output-escaper (flatten (walk x visitor)))))

(serialize [
            " numbers: " 1 2 3.0 
            " vec: " ["->-"]
            " list: " '(1 2 3)
            " seq: " (seq [1 2 3])
            " nil: " nil
            " symbol: " 'Foo
            " maps: " {:a 1}
            " keywords: " :a " " :foo/b
            ])
