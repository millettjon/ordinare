(ns ordinare.tree
  (:require
   [babashka.fs        :as fs]
   [clojure.spec.alpha :as s]
   [ordinare.module    :as module]
   [ordinare.spec      :as o.s]))

(defn pre-walk-pc
  "Walks tree from root `node` in depth first order pre replacing node
  with (f nil node) for the root and (f parent node) for any
  children."
  ([node f]
   (pre-walk-pc nil node f))
  ([parent node f]
   (when node
     (let [{:keys [children] :as self} (f parent node)
           update-child                #(pre-walk-pc self % f)]
       (cond-> self
         children (update :children #(mapv update-child %)))))))

(defn pre-walk
  [node f]
  (pre-walk-pc node #(f %2)))

(defn post-walk
  "Walks tree in depth first order post applying f to each node."
  [tree f]
  (when tree
    (let [children (:children tree)]
      (cond-> tree
        children (assoc :children (mapv #(post-walk % f) children))
        true f))))

;; Specs for hiccup style configuration.
(s/def ::type     #(or (keyword? %) (string? %)))
(s/def ::options  map?)
(s/def ::argument #(or (-> % vector? not)
                       (-> % meta :ordinare/arg)))
(s/def ::child    vector?)
(s/def ::node     (s/cat :type     ::type
                         :opts     (s/? ::options)
                         :args     (s/* ::argument)
                         :children (s/* ::child)))

(defn- normalize-node
  [node]
  (let [{k-or-path :type
         :as result} (o.s/conform-or-throw ::node node)]
    (cond-> result
      (string? k-or-path) (-> (assoc :type :directory)
                              (assoc-in [:opts :path] k-or-path)))))

(comment
  (normalize-node ["src/foo" {:bar "BAR"}])
  (normalize-node [:foo {:bar "BAR"}])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3 [:foo]])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3 ^:ordinare/arg [:foo] [:bar]]))

(defn normalize
  "Takes a node in hiccup format and returns it in normalized form."
  [node]
  (pre-walk node normalize-node))

(comment
  (normalize ["src"])
  (normalize ["src/foo"])
  (normalize ["src/foo" {:bar "BAR"}])
  (normalize [:foo {:bar "BAR"}])
  (normalize ["src/foo" {:bar "BAR"} 1 2 3]) ; args
  (normalize ["src/foo" {:bar "BAR"} 1 2 3 [:foo]]) ; children
  (normalize ["src/foo" {:bar "BAR"} 1 2 3 ^:ordinare/arg [:foo] ; vector arg
              [:bar]]))

(defn ensure-required
  [node]
  (pre-walk node module/ensure-required))

(defn assert-valid
  "Validates node and any children."
  [node]
  (pre-walk node module/assert-valid))

(comment
  (-> ["src"]
      normalize
      assert-valid))

(defn- add-context-to-children
  [parent node]
  (let [p-context (:context parent)

        level
        (if-let [l (:level p-context)]
          (inc l)
          0)

        path
        (when-let [path (-> parent :opts :path)]
          (str (fs/path (:path p-context) path)))

        context
        (cond-> {:level level}
          path (merge {:path path}))]

    (assoc node :context context)))

(defn add-context
  "Adds a context to each node based on the path and level of the node's parent."
  [node]
  (pre-walk-pc node add-context-to-children))
(comment
  (-> ["src"] normalize add-context assert-valid)
  (-> ["src"
       ["ordinare"]]
      normalize add-context assert-valid)
  (-> ["src"
       ["ordinare"
        ["foo"]]]
      normalize add-context assert-valid))

(defn init-from-config
  [node]
  (-> node
      normalize
      ensure-required
      assert-valid
      add-context))

(defn select
  "Marks nodes in tree as selected if (f node) returns true."
  [node f]
  (pre-walk node #(cond-> %
                    (f %)
                    (assoc :selected? true))))

(comment
  ;; ? should select propagate to children?
  (-> ["src"
       ["foo"
        ["bar"]]]
      normalize
      add-context
      assert-valid
      (select any?))
  )

;; clj-kondo bug: unsupported escape char mis-identified as invalid escape char
;; (defn foo
;;     [x]
;;     (str "\-" x))
