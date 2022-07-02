(ns ordinare.effect
  (:require
   [clojure.string :as str]
   [ordinare.process :as process]))

(defn warn
  [message]
  {:message (format "WARNING: %s" message)})

(defn $
  [& args]
  {:fn      #(apply process/$ args)
   :message (str "$ " (str/join " " args))})

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
