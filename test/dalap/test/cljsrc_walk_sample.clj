^{:cljs
  (ns dalap.cljs.test.cljsrc-walk)}
(ns dalap.test.cljsrc-walk)

^:clj
(do
  (defn only-in-clj [] true)
  (defn only-in-clj2 [i] (* i 2))
  )
(defmacro clj-only-macro [sadf]
  nil)
(comment
  'asdf)
(defn foo []
  ^{:cljs something-else-cljs}
  foobar
  (Error. "asdf")
  (Error "Fooe")
  clojure.lang.Atom
  something)

#_(cljs
   (just-testing something)

   (defn cljs-func []
     nil)
   )

#_(
   (read-file (LineNumberingPushbackReader. reader))
   )
