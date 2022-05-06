(ns ordinare.util
  (:require
   [clojure.data :as d]))

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

(defn- diff->op
  [op m k v]
  (assoc-in m [k op] v))

(def ^:private diff->+
  (partial diff->op :+))

(def ^:private diff->-
  (partial diff->op :-))

(defn diff-map
  "Returns a diff of a flattenend map."
  [a b]
  (let [[in-a in-b in-both] (d/diff a b)]
    (as-> in-both $
      (reduce-kv diff->- $ in-a)
      (reduce-kv diff->+ $ in-b))))
