(ns ordinare.modules.firefox
  (:require
   [ordinare.conf.ini :as ini]
   [ordinare.log :as log]
   [ordinare.module :as module]))

(comment
  (ini/read "/home/jam/.mozilla/firefox/profiles.ini")
  )

;; check if profile exists
;; -? does a profile need to be created if missing?
;; - install add on in profile
;; - set dark theme in profile
;; - set search engines in profile

#_(defn get-profile-uuid
    [profile]
    (gs/get-setting "org.gnome.Terminal.ProfilesList" profile))

#_(defn get-profile-path
  [profile-uuid]
  (format "org.gnome.Terminal.Legacy.Profile:/org/gnome/terminal/legacy/profiles:/:%s/" profile-uuid))

(defmethod module/configure :firefox
  [conf]
  (log/debug "loaded module" conf)
  (doseq [profile (-> conf :profiles keys)
          :let    [path (-> profile get-profile-uuid get-profile-path)]
          [k v]   (get-in conf [:profiles profile])
          :let    [v-old (gs/get-setting path k)]]
    (when (not= v v-old)
      (log/info "configured setting" {profile {k v}})
      (gs/set-setting path k v))))
