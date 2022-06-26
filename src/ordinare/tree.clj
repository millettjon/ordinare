(ns ordinare.tree
  (:require
   [babashka.fs        :as fs]
   [clojure.spec.alpha :as s]
   [ordinare.module    :as module]
   [ordinare.module.directory :refer [directory]]
   [ordinare.spec      :as o.s]
   [ordinare.util      :as u]))

(defn pre-walk-pc
  "Walks tree from root `node` in depth first order pre replacing node
  with (f nil node) for the root and (f parent node) for any
  children."
  ([node f]
   (pre-walk-pc nil node f))
  ([parent node f]
   (when node
     (let [{:keys [ord/children] :as self} (f parent node)
           update-child                #(pre-walk-pc self % f)]
       (cond-> self
         children (update :ord/children #(mapv update-child %)))))))

(defn pre-walk
  [node f]
  (pre-walk-pc node #(f %2)))

(defn post-walk
  "Walks tree in depth first order post applying f to each node."
  [tree f]
  (when tree
    (let [children (:ord/children tree)]
      (cond-> tree
        children (assoc :ord/children (mapv #(post-walk % f) children))
        true f))))

;; Specs for hiccup style configuration.
(s/def ::type keyword?)
(s/def ::fn   fn?)
(s/def ::spec #(or (s/spec? %)
                   (and (keyword? %) (namespace %))))
(s/def ::name string?)
(s/def ::module (s/keys :req-un [::type ::fn]
                        :opt-un [::spec ::name]))

(s/def ::tag      (s/alt :module ::module, :path string?))
(s/def ::options  map?)
(s/def ::argument #(or (-> % vector? not)
                       (-> % meta :ord/arg)))
(s/def ::child    vector?)
(s/def ::node     (s/cat :tag          ::tag
                         :opts         (s/? ::options)
                         :args         (s/* ::argument)
                         :ord/children (s/* ::child)))

(defn- normalize-node
  [node]
  (let [{:keys [tag opts]
         :as   result} (o.s/conform-or-throw ::node node)]
    (-> result
        ;; merge :opts into top level
        (dissoc :opts)
        (merge opts)

        (dissoc :tag)
        (merge (let [[k v] tag]
                 (case k
                   ;; sugar for a directory
                   :path (-> directory
                             (u/qualify-keys :ord)
                             (assoc :path v))
                   :module (u/qualify-keys v :ord)))))))

(comment
  (normalize-node ["src/foo" {:bar "BAR"}])
  (normalize-node [{:type :foo, :fn identity} {:bar "BAR"}])
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
  (let [p-context (:ord/context parent)

        level
        (if-let [l (:level p-context)]
          (inc l)
          0)

        path
        (when-let [path (-> parent :path)]
          (str (fs/path (:path p-context) path)))

        context
        (cond-> {:level level}
          path (merge {:path path}))]

    (assoc node :ord/context context)))

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
      assert-valid
      add-context))

(defn select
  "Marks nodes in tree as selected if (f node) returns true."
  [node f]
  (pre-walk node #(cond-> %
                    (f %)
                    (assoc :ord/selected? true))))

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
