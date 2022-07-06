(ns ordinare.store
  (:require
   [ordinare.fs     :as fs]
   [ordinare.config :refer [*config*]])
  (:refer-clojure :exclude [resolve]))

(def STORE "store")

(defn store-dir
  []
  (fs/path (-> *config* :config-dir) STORE))

(defn resolve
  [{:keys [path] :as module}]
  (let [store-path (-> (store-dir)
                       (fs/path (-> module :ord/context :path) path)
                       str)]
    (when (fs/exists? store-path)
      store-path)))
