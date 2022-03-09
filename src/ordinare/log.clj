(ns ordinare.log
  (:require
   [clojure.string :as str]))

(def ^:dynamic *level* :info)

(def levels
  (zipmap
   [:debug :info :warning :error]
   (range)))

;; TODO save time
;; TODO support exceptions
;; TODO support thread local context?
;; TODO levels per ns?

;; TODO add console pretty printer
;; - abbreviate ns: ordinare.modules.git -> o.m.git
;; - use color to display level: dbg(gray) inf(white) wrn(yellow) err(red)
;; - align columns (fit to fixed width? align around center? use window size of terminal? use ncurses lib?)
;;                  o.cli arguments     {:ordinare/config-dir
;;                                       :configure true, :status false}
;;                o.m.git loaded module {:ordinare/module :git, :user {:email "jon@millett.net", :name "Jonathan Millett"}}
;;     DBG o.m.gnome-terminal loaded module {:ordinare/module :gnome-terminal, :profiles {"default" {:use-system-font false, :font "Monospace 14", :use-theme-colors false, :background-color "#000000", :foreground-color "#D0D0D0", :audible-bell false}}}
;; - syntax highlight edn data
;; - pretty print edn data

(defn log
  ([level ns string-or-data]
   (if (string? string-or-data)
     (log level ns string-or-data nil)
     (log level ns nil string-or-data)))
  ([level ns message data]
   (->> [(-> ns str)
         (-> level name str/upper-case)
         message
         (when data (pr-str data))]
        (remove nil?)
        (apply println))))

(defmacro deflevel
  [level]
  `(defmacro ~(symbol level)
     ([string-or-data]
      `(when (>= ~(~level levels) (*level* levels))
         (log ~~level ~*ns* ~string-or-data)))
     ([message data]
      `(when (>= ~(~level levels) (*level* levels))
         (log ~~level ~*ns* ~message ~data)))))

;; for clj-kondo
(declare debug)
(declare info)
(declare warning)
(declare error)

(doseq [level (keys levels)]
  (eval `(deflevel ~level)))
#_(error "xyz" {:foo "FF"})
