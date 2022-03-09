(ns ordinare.module
  (:refer-clojure :exclude [require]))

(defn require
  [module-conf]
  (-> module-conf
      :ordinare/module
      name
      (->> (str "ordinare.modules."))
      symbol
      clojure.core/require))

(defmulti configure :ordinare/module)
