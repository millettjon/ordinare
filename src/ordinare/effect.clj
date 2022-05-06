(ns ordinare.effect
  (:require
    [ordinare.module :refer [dispatch]]))

(defn notify-user
  [module message effect]
  (println (str "(" (-> module :type name) ")") message (pr-str effect)))

(defmulti delete!-impl dispatch)

(defn delete!
  [module effect]
  (delete!-impl module effect)
  (notify-user module "Deleted setting:" effect))

(defmulti add!-impl dispatch)

(defn add!
  [module effect]
  (add!-impl module effect)
  (notify-user module "Added setting:" effect))

(defmulti update!-impl dispatch)

(defn update!
  [module effect]
  (update!-impl module effect)
  (notify-user module "Updated setting:" effect))

(defn apply!
  [module effect]
  (let [ops (-> effect val keys set)
        f (case ops
            #{:-}    delete!
            #{:+}    add!
            #{:+ :-} update!)]
    (f module effect)))
