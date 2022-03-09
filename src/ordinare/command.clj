(ns ordinare.command
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [ordinare.module :as module]))

;; TODO
(defn status
  [arg-map]
  (prn "status" arg-map))

(defn read-config
  [arg-map]
  (-> arg-map
      :ordinare/config-dir
      (fs/path "config.edn")
      str
      slurp
      edn/read-string))

(defn find-modules
  [conf module-names]
  (let [module-kws (->> module-names (map keyword) set )]
    (filter #(module-kws (:ordinare/module %)) conf)))

(defn configure
  [{modules :module
    :as arg-map}]
  (let [conf (read-config arg-map)
        modules (cond-> (:modules conf)
                  (seq modules) (find-modules modules))]
    (doseq [module-conf modules]
      (module/require module-conf)
      (module/configure module-conf))))
#_ (module/require {:ordinare/module :git})
