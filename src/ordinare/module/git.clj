(ns ordinare.module.git
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string     :as str]
   [ordinare.fs        :as fs]
   [ordinare.effect    :as effect]
   [ordinare.process   :refer [$]]))

;; ----------
;; EFFECT HELPERS

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

;; ----------
;; CONFIG MODULE

(s/def ::name       string?)
(s/def ::email      string?)
(s/def ::user       (s/keys :opt-un [::name ::email]))

(def config
  {:type :git/config
   :spec (s/keys :opt-un [::user])
   :name "git config"
   :fn   (fn [{:keys [user]}]
           (mapv (fn [[k expected]]
                   (let [actual (get-global-setting [:user k])]
                     (when (not= actual expected)
                       {:fn      #(set-global-setting [:user k] expected)
                        :message (format "set user.%s: %s -> %s" (name k) actual expected)})))
                 user))})

;; ----------
;; CLONE MODULE

(defn top-level
  "Returns the top level git directory."
  []
  (try
    (-> ($ "git" "rev-parse" "--show-toplevel")
        first)
    (catch Exception _)))

(defn repo?
  "Returns true if dir is a git repo."
  []
  (= (top-level) fs/*cwd*))

(defn origin-url
  []
  (-> ($ "git" "config" "--get" "remote.origin.url")
      first))

(s/def ::url string?)

(def clone
  {:type :git/clone
   :spec (s/keys :opt-un [::url])
   :name "git clone"
   :fn   (fn
           [{:keys [url]}]
           (cond
             ;; Directory is empty.
             (fs/empty? ".")
             [{:fn      #($ "git" "clone" url ".")
               :message url}]

             ;; A repo already exists.
             (repo?)
             (let [actual-url (origin-url)]
               (if (= url actual-url)
                 []                     ; url matches, we are good
                 [(effect/warn (format "repo already exists with origin %s" actual-url))]))

             ;; Directory exists but not empty or a repo.
             :else
             [(effect/warn "directory already exists")]
             ))})
