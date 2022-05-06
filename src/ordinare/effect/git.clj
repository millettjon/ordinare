(ns ordinare.effect.git
  (:require
   [clojure.string   :as str]
   [ordinare.process :refer [$]]))

;; ----------
;; SETTINGS

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

(defn repo?
  "Returns true if dir is a git repo."
  [dir]
  (try
    (let [result ($ {:dir dir} "git" "rev-parse" "--show-toplevel")]
      (= dir (first result)))
    (catch Exception _)))
#_ (repo? "/home/jam/src/luminare")

(defn origin-url
  [dir]
  (-> {:dir dir}
      ($  "git" "config" "--get" "remote.origin.url")
      first))
#_ (origin-url "/home/jam/src/luminare")
