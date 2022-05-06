(ns ordinare.module
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.log       :as log]
   [soothe.core        :as sth])
  (:refer-clojure :exclude [require]))

(defn get-ns
  [module-conf]
  (-> module-conf
      :type
      name
      (->> (str "ordinare.module."))))
#_ (get-ns {:ordinare/module :git})

(defn require
  [module-conf]
  (-> module-conf
      get-ns
      symbol
      clojure.core/require)
  (log/debug "loaded module" module-conf))
#_ (require {:ordinare/module :git})

;; Keeping this generic to work with different multi methods.
;; It also helps keep the signatures consistent.
(defn dispatch
  [module & _args]
  (module :type))

(defn assert-valid
  [module]
  (let [spec (-> module
                 get-ns
                 (keyword "config"))]
    (or (s/valid? spec module)
        (throw (ex-info (str "invalid " spec) (sth/explain-data spec module))))))
#_ (assert-valid {:type :git})

(defmulti query
  "Queries the live system and returns the actual configuration state."
  dispatch)

(defmulti diff
  dispatch)

(defmulti configure!
  "Applies a diff of changes to the live system."
  dispatch)
