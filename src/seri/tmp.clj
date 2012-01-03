
  ;; clojure.lang.Keyword (ser [kw w]
  ;;                        (if (nil? (namespace kw))
  ;;                          (w (name kw))
  ;;                          (w [(namespace kw) "/" (name kw)])))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (def dalapm nil)
;; (defmulti dalapm (fn [o w] (type o)))
;; (defmethod dalapm Number [n w] (str (* 2 n)))
;; (defmethod dalapm :default [s w] (ser s w))

;; (defmulti dalap2 class)
;; (defmacro defmethod*
;;   [multifn & dispatch-pairs]
;;   ;; `(do
;;   ;;    (defmethod dalap2 Number [n#] (fn [n# w#] (str n#)))
;;   ;;    (defmethod dalap2 String [s#] (fn [s# w#] s#))
;;   ;;    )
;;   `(do
;;      ~@(for [[dispatch-val fn-tail] (partition 2 dispatch-pairs)]
;;          `(defmethod ~(with-meta multifn {:tag 'clojure.lang.MultiFn})
;;             ~dispatch-val [o#] ~@fn-tail)))
;;   )
;; (defmethod* dalap2
;;   Number (fn [n w] (str n))
;;   String (fn [s w] s)
;;   :default (fn [o w] "NOT FOUND")
;;   )


;; (extend-dalap
;;  clojure.lang.IPersistentMap
;;  (fn [m w] (for [[k v] m] [k ": " v]) ))



;; (visit [x walker] ....)

;; (defmethod extended-visit :default [x w] (std-visit x w))
;; (deftype NewVisitorWrapper [contents]
;;   Serializable
;;   (visit [nvw w] (walk contents extended-visit)))

;; [:div 123 "asdfl & " (NewVisitorWrapper. ["asdfasdf"])]



;; ;;; fixed visitor func
;; (visit [N] (str N))
;; (visit [s] (map visit s))


;; (defprotocol RomansSeri
;;   (roman-visit [o w]))

;; (extend-protocol RomansSeri
;;   Object (roman-visit [o w] (visit o w))
;;   nil (roman-visit [_ _] "")
;;   TagAttrs (roman-visit [ta w]
;;              (visit (TagAttrs. (.tag ta) (assoc (.attrs-map ta) :new 'foo))
;;                     w)))
