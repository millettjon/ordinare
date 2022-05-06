(ns ordinare.spec
  (:require
   [clojure.spec.alpha :as s]
   [soothe.core        :as sth]))

;; Ref: https://groups.google.com/g/clojure/c/fti0eJdPQJ8
(defmacro only-keys
  "Returns spec for a closed map."
  [& {:keys [req req-un opt opt-un] :as args}]
  `(s/merge (s/keys ~@(apply concat (vec args)))
            (s/map-of ~(set (concat req
                                    (map (comp keyword name) req-un)
                                    opt
                                    (map (comp keyword name) opt-un)))
                      any?)))

(defn conform-or-throw
  [spec x]
  (let [result (s/conform spec x)]
    (when (= result ::s/invalid)
      (throw (ex-info (str "invalid " spec) (assoc (sth/explain-data spec x) :x x))))
    result))

(s/def ::level int?)
(s/def ::path string?)
(s/def ::context (only-keys :req-un [::path ::level]))
