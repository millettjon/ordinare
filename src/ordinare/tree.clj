(ns ordinare.tree
  (:require
   [babashka.fs        :as fs]
   [clojure.spec.alpha :as s]
   [ordinare.spec      :as o.s]))

;; Spec for hiccup style configuration
(s/def ::type     #(or (keyword? %) (string? %)))
(s/def ::options  map?)
(s/def ::argument #(or (-> % vector? not)
                       (-> % meta :ordinare/arg)))
(s/def ::child    vector?)
(s/def ::node     (s/cat :type     ::type
                        :opts     (s/? ::options)
                        :args     (s/* ::argument)
                        :children (s/* ::child)))
(defn normalize
  [node]
  (let [{k-or-path :type
         :as result} (o.s/conform-or-throw ::node node)]
    (cond-> result
      (string? k-or-path) (-> (assoc :type :directory)
                              (assoc-in [:opts :path] k-or-path)))))

(comment
  (normalize ["src/foo" {:bar "BAR"}])
  (normalize [:foo {:bar "BAR"}])

  (normalize ["src/foo" {:bar "BAR"} 1 2 3])
  (normalize ["src/foo" {:bar "BAR"} 1 2 3 [:foo]])
  (normalize ["src/foo" {:bar "BAR"} 1 2 3 ^:ordinare/arg [:foo] [:bar]]))

(defn nested-context
  [{:keys [context]
    {:keys [path]} :opts}]
  (cond-> context
    true (update :level inc)
    path (update :path #(-> %
                            (fs/path path)
                            fs/normalize
                            str))))

(comment
  (nested-context  {:context {:path "src" :level 0}
                    :opts {:path "foo"}}))

(defn module-seq
  [context tree]
  (when tree
    (let [context (assoc context :level 0)
          modules [(-> tree
                       normalize
                       (assoc :context context))]]
      ((fn mod-seq
         [modules]
         (when modules
           (lazy-seq
            (let [module   (first modules)
                  children (when-let [children (:children module)]
                             (let [nc (nested-context module)]
                               (map #(-> %
                                         normalize
                                         (assoc :context nc))
                                    children)))
                  modules  (->> modules rest (concat children) seq)]
              (when module
                (cons module (mod-seq modules)))))))
       modules))))

(comment
  (module-seq {:path "."} nil)
  (module-seq {:path "."} ["foo" {:bar "BAR"}])

  (module-seq {:path "."} ["src" {:bar "BAR"}
                         ["ordinare2" 1 2 3]])

  (module-seq {:path "."} ["src" {:bar "BAR"}
                         ["ordinare2"
                          [:symlink ]]]))
