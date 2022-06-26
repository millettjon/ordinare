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

(defn flatten-map
  "Flattens a nested map."
  [nested-map]
  (loop [m  {}
         xs [[[] nested-map]]]
    (let [[path x] (first xs)
          rxs      (rest xs)]
      (cond
        ;; return result if nothing left to process
        (empty? xs)
        m

        ;; convert map to vec of map-entries
        (map? x)
        (recur m (into rxs (mapv (fn [[k v]] [(conj path k) v]) x)))

        ;; anything else is a leaf
        :else
        (recur (assoc m path x) rxs)))))
