(ns ordinare.module
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.config    :refer [*config*]]
   [ordinare.fs        :as fs]
   [ordinare.log       :as log]
   [soothe.core        :as sth]))
#_(remove-ns 'ordinare.module)

(defn path-matches?
  [module target-path]
  (let [cp   (-> module :context :path)
        mp   (-> module :opts :path)
        path (->> [cp mp]
                  (remove nil?)
                  (apply fs/path)
                  fs/normalize)]
    (fs/starts-with? path target-path)))

(defn get-ns
  [module]
  (-> module
      :type
      name
      (->> (str "ordinare.module."))))
#_ (get-ns {:ordinare/module :git})

(defn ensure-required
  [module]
  (let [sym (-> module
                get-ns
                symbol)]
    (when-not (find-ns sym)
      (require sym)
      (log/debug "loaded module" module))
    module))
#_ (ensure-required {:type :directory})

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
    (when-not (s/valid? spec module)
      (throw (ex-info (str "invalid " spec) (sth/explain-data spec module))))
    module))
#_ (assert-valid {:type :git})

(defn context-dir
  [node]
  (->> [(:work-dir *config*)
        (-> node :context :path)]
       (remove nil?)
       (apply fs/path)
       fs/normalize
       str))

(defn with-context-dir
  [module f]
  (fs/with-cwd (context-dir module)
    (f module)))

(defmulti diff*
  "Returns a diff of effects required to actualize the specified configuration."
  dispatch)

(defn diff
  [module]
  (with-context-dir module diff*))
