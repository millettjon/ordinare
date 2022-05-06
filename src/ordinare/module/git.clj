(ns ordinare.module.git
  (:require
   [clojure.spec.alpha  :as s]
   [medley.core         :refer [filter-vals]]
   [ordinare.effect     :as effect]
   [ordinare.effect.git :as git]
   [ordinare.module     :as module]
   [ordinare.spec       :as o.s :refer [only-keys]]
   [ordinare.util       :as u]))

(def MODULE :git)

;; ----------
;; SPECS
(s/def ::type   #(= MODULE %))
(s/def ::name   string?)
(s/def ::email  string?)
(s/def ::user   (only-keys :req-un [::name ::email]))
(s/def ::opts   (only-keys :req-un [::user]))
(s/def ::config (only-keys :req-un [::type ::o.s/context ::opts]))

#_ {:type :git,
    :opts {:user {:email "jon@millett.net", :name "Jonathan Millett"}}}

(defmethod module/query MODULE
  [module]
  {:user (reduce-kv (fn [m k _v]
                      (if-let [v (git/get-global-setting [:user k])]
                        (assoc m k v)
                        m))
                    {}
                    (-> module :opts :user))})

(defmethod module/diff MODULE
  [module current-state]
  (filter-vals map?
               (u/diff-map
                (u/flatten-map current-state)
                (-> module
                    :opts
                    u/flatten-map))))

(defmethod effect/update!-impl MODULE
  [_module [ks {new-v :+}]]
  (git/set-global-setting ks new-v))

(defmethod effect/add!-impl MODULE
  [_module [ks {new-v :+}]]
  (git/set-global-setting ks new-v))
