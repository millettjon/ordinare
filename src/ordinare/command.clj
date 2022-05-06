(ns ordinare.command
  (:require
   [clojure.pprint  :refer [pprint]]
   [ordinare.module :as    module]
   [ordinare.module.default]))

(defn do-modules
  [{:keys [modules]
    :as   _arg-map}
   f]
  (doseq [module modules]
    (module/require module)
    (module/assert-valid module)
    (f module)))

(defn status
  [arg-map]
  (do-modules
   arg-map
   (fn [module]
     (let [current-state (module/query module)
           diff          (module/diff module current-state)]
       (when (seq diff)
         (println "----------")
         (println "module:" (-> module :type name))
         (pprint diff))))))

(defn configure
  [arg-map]
  (do-modules arg-map module/configure!))
