(ns ordinare.module
  (:refer-clojure :exclude [require]))

(defn require
  [module-conf]
  (-> module-conf
      :ordinare/module
      name
      (->> (str "ordinare.module."))
      symbol
      clojure.core/require))

(defmulti configure
  (fn [_conf module]
    (module :ordinare/module)))
