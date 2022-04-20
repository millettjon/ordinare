(ns ordinare.command
  (:require
   [clojure.pprint :refer [pprint]]
   [ordinare.module :as module]))

(defn status
  [conf _arg-map]
  (-> conf
      (update :modules (partial mapv :ordinare/module))
      pprint))

(defn configure
  [conf
   {:keys [modules]
    :as _arg-map}]
  (doseq [module-conf (or modules (:modules conf))]
      (module/require module-conf)
      (module/configure conf module-conf)))
#_ (module/require {:ordinare/module :git})
