(ns ordinare.module.gnome
  (:require
   [clojure.spec.alpha        :as s]
   [ordinare.effect.gsettings :as gs]))

(defn get-profile-uuid
  [profile]
  (gs/get-setting "org.gnome.Terminal.ProfilesList" profile))

(defn get-profile-path
  [profile-uuid]
  (format "org.gnome.Terminal.Legacy.Profile:/org/gnome/terminal/legacy/profiles:/:%s/" profile-uuid))

(s/def ::profiles map?)

(def terminal
  {:type :gnome/terminal
   :name "gnome-terminal"
   :spec (s/keys :req-un [::profiles])
   :fn   (fn [{:keys [profiles]}]
           (for [[profile settings] profiles
                 :let    [path (-> profile get-profile-uuid get-profile-path)]
                 [k v]   settings
                 :let    [v-old (gs/get-setting path k)]]
             (when (not= v v-old)
               {:fn #(gs/set-setting path k v)
                :message (format "set %s = %s (was %s)" (name k) v v-old)})))})
