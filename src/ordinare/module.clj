(ns ordinare.module
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.config    :refer [*config*]]
   [ordinare.fs        :as fs]
   [soothe.core        :as sth]))
#_(remove-ns 'ordinare.module)

(defn path-matches?
  [module target-path]
  (let [cp   (-> module :ord/context :path)
        mp   (-> module :path)
        path (->> [cp mp]
                  (remove nil?)
                  (apply fs/path)
                  fs/normalize)]
    (fs/starts-with? path target-path)))

(defn assert-valid
  [module]
  (when-let [spec (:ord/spec module)]
    (or (s/valid? spec module)
        (throw (ex-info (str "invalid " spec) (sth/explain-data spec module)))))
  module)

(defn context-dir
  [module]
  (->> [(:work-dir *config*)
        (-> module :ord/context :path)]
       (remove nil?)
       (apply fs/path)
       fs/normalize
       str))

;; For repl use.
(defn start!
  []
  (alter-var-root #'fs/*cwd* (constantly (context-dir nil))))
#_ (start!)

(defn with-context-dir
  [module f]
  (fs/with-cwd (context-dir module)
    (f module)))

(defn diff
  [module]
  (let [effects (with-context-dir module
                  (:ord/fn module))]
    (->> effects
         (remove nil?)
         seq)))
