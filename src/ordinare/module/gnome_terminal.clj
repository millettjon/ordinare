(ns ordinare.module.gnome-terminal
  (:require
   [ordinare.effect.gsettings :as gs]
   [ordinare.log :as log]
   [ordinare.module :as module]))

(defn get-profile-uuid
  [profile]
  (gs/get-setting "org.gnome.Terminal.ProfilesList" profile))

(defn get-profile-path
  [profile-uuid]
  (format "org.gnome.Terminal.Legacy.Profile:/org/gnome/terminal/legacy/profiles:/:%s/" profile-uuid))

(defmethod module/configure :gnome-terminal
  [conf]
  (log/debug "loaded module" conf)
  (doseq [profile (-> conf :profiles keys)
          :let    [path (-> profile get-profile-uuid get-profile-path)]
          [k v]   (get-in conf [:profiles profile])
          :let    [v-old (gs/get-setting path k)]]
    (when (not= v v-old)
      (log/info "configured setting" {profile {k v}})
      (gs/set-setting path k v))))
