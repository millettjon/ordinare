(ns ordinare.conf.gsettings
  (:require
   [ordinare.process :refer [$]]))

;; Settings are stored in:
;; $XDG_CONFIG_HOME/dconf (i.e., ~/.config/dconf by default)
;; ~/.config/dconf/user
;;
;; DUMP SETTINGS
;; dconf dump /

(defn ->clj
  "Coerce setting string x to clojure value."
  [x]
  (condp re-matches x
    #"'(.*)'"        :>> second
    #"(true|false)"  (Boolean. x)
    #"[+-]?\d+"      (Long. x)
    #"[+-]?\d+\.\d+" (Double. x)
    x))

(defn ->setting
  "Coerce clojure val x to setting string."
  [x]
  (if (string? x)
    (format "'%s'" x)
    (str x)))

(defn get-setting
  [path key]
  (-> ($ "gsettings" "get" path (name key))
      first
      ->clj))

(defn set-setting
  [path key val]
  ($ "gsettings" "set" path (name key) (->setting val)))

(defn get-settings
  [path]
  (reduce (fn [m k]
            (assoc m (keyword k) (get-setting path k)))
          {}
          ($ "gsettings" "list-keys" path)))
