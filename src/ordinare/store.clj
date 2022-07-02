(ns ordinare.store
  (:require
   [babashka.fs     :as fs]
   [ordinare.config :refer [*config*]])
  (:refer-clojure :exclude [resolve]))

(def STORE "store")

(defn store-dir
  []
  (fs/path (-> *config* :config-dir) STORE))

(defn resolve
  [path]
  (str (fs/path (store-dir) path)))
#_ (resolve "foo")
