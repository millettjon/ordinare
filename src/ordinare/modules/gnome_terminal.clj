(ns ordinare.modules.gnome-terminal
  (:require
   [ordinare.conf.gsettings :as gs]))

(defn get-profile-uuid
  [profile]
  (gs/get-setting "org.gnome.Terminal.ProfilesList" profile))

(defn get-profile-path
  [profile-uuid]
  (format "org.gnome.Terminal.Legacy.Profile:/org/gnome/terminal/legacy/profiles:/:%s/" profile-uuid))

;; ? does a protocol make sense? implemented as multi methods under the hood
;; ? does babashka work with protocols?
(defn apply-conf
  [conf]
  (doseq [profile (-> conf :profiles keys)
          :let    [path (-> profile get-profile-uuid get-profile-path)]
          [k v]   (get-in conf [:profiles profile])
          :let    [v-old (gs/get-setting path k)]]
    (when (not= v v-old)
      (prn "setting" {profile {k v}})
      (gs/set-setting path k v))))

(comment
  (apply-conf {:profiles
               {"default"
                {:use-system-font  false
                 :font             "Monospace 14"
                 :use-theme-colors false
                 :background-color "#000000"
                 :foreground-color "#D0D0D0"
                 :audible-bell     false}}}))
