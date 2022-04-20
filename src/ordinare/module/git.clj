(ns ordinare.module.git
  (:require
   [clojure.string :as str]
   [ordinare.log :as log]
   [ordinare.module :as module]
   [ordinare.process :refer [$]]))

(defn ->key-path
  [ks]
  (->> ks
       (map name)
       (str/join ".")))
#_ (->key-path [:foo :bar])

(defn get-global-setting
  [ks]
  (try
    (->> (->key-path ks)
         ($ "git" "config" "--global" )
         first)
    (catch Exception _)))
#_ (get-global-setting [:user :email])

(defn set-global-setting
  [ks v]
  ($ "git" "config" "--global" (->key-path ks) v))
#_ (set-global-setting [:user :email] "jon@millett.net")

(defmethod module/configure :git
  [conf]
  (log/debug "loaded module" conf)
  (doseq [[k v] (conf :user)
          :let  [ks    [:user k]
                 old-v (get-global-setting ks)]]
    (when-not (= v old-v)
      (log/info "configured setting" {k v})
      (set-global-setting ks v))))
