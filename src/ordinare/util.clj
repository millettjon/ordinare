(ns ordinare.util)

(defn qualify-keys
  "Qualifies keys in a map with the given namespace."
  [m namespace]
  (let [n (name namespace)]
    (reduce-kv (fn [m k v]
                 (assoc m (keyword n (name k)) v))
               {} m)))
(comment
  (qualify-keys {:food :oysters} "bar")
  (qualify-keys {:food :oysters} :bar))
