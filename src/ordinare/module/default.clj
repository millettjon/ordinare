(ns ordinare.module.default
  (:require
   [ordinare.effect :as effect]
   [ordinare.module :as module]))

(defmethod module/configure! :default
  [module]
  (let [effects (->> module
                     module/query
                     (module/diff module))]
    (doseq [effect effects]
      (effect/apply! module effect))))
