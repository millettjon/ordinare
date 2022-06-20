(ns ordinare.effect)

(defn- dispatch
  [{module-type :type :as _module}
   [effect-type :as _effect]]
  (if (namespace effect-type)
    effect-type
    (-> module-type
        name
        (->> (str "ordinare.module."))
        (keyword effect-type))))

(defmulti ->str dispatch)

(defmethod ->str :default
  [module effect]
  (let [effect' (update effect 0 (comp keyword name))]
    (str "(" (-> module :type name) ") " (pr-str effect'))))

(defmulti apply*! dispatch)

;; TODO works in cider repl but gnome-terminal shows as "?"
(def white-check "\u2705")
;; bb -e '(System/setProperty "file.encoding" "UTF-8") (println "\u2705")'
;; echo -e "\xe2\x96\x88"

;; None of these work
#_(System/setProperty "file.encoding" "UTF-8")
#_(System/setProperty "file.encoding" "UTF8")
#_(let [writer (clojure.java.io/writer *out* :encoding "UTF-8")]
  (.write writer "checkmark: \u2705 \n\n")
  (.flush writer))

;; This seems wrong.
#_(System/getProperty "file.encoding")    ; "ANSI_X3.4-1968"
#_(System/setProperty "file.encoding" "UTF-8")

;; However, this works somehow in cider-repl.
#_(print white-check)

(defn apply!
  [module effect]
  (try
    ;; TODO add logging
    {:ok true
     :value (apply*! module effect)
     :message "+" #_white-check}
    (catch Exception ex
      {:ok false
       :message (str ex)
       :value ex})))
