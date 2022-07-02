(ns ordinare.cli
  (:require
   [docopt.core      :as docopt]
   [ordinare.command :as command]
   [ordinare.config  :refer [*config*]]
   [ordinare.log     :as log]
   [ordinare.module  :as module]
   [ordinare.tree    :as tree]
   [ordinare.config :as config]))

;; Ref: http://docopt.org/
(def usage "ordinare - organize directory

Usage:
  ord [options] status [<path-or-module> ...]
  ord [options] apply  [<path-or-module> ...]

Options:
  -v --verbose
  ")

(defn init-modules
  "Initializes the tree of modules from the config and command line arguments."
  [arg-map]
  (let [targets (-> arg-map :path-or-module set)]
    (-> arg-map
        (assoc :tree (-> *config*
                         :root
                         tree/init-from-config
                         (tree/select
                          (fn [module]
                            (or (empty? targets) ; match everything if no targets
                                (some (partial module/path-matches? module) targets)
                                (targets (-> module :ord/type str #_name)))))))
        (dissoc :path-or-module))))

(defn normalize-key
  [s]
  (let [s (condp re-matches s
            #"<(.+)>" :>> second
            #"--(.+)" :>> second
            s)]
    (keyword s)))

(defn normalize-keys
  [arg-map]
  (reduce-kv
   (fn [m k v]
     (assoc m (normalize-key k) v))
   {}
   arg-map))

(defn handle-verbose
  [arg-map]
  (when (:verbose arg-map)
    (alter-var-root #'log/*level* (constantly :debug)))
  (dissoc arg-map :verbose))

(defn dispatch-command
  [arg-map]
  (log/debug "arguments" arg-map)
  ((condp #(some %2 %1) arg-map
     #{:status} command/status
     #{:apply}  command/apply)
   arg-map))

(defn process-args
  [arg-map]
    #_(println "*CONFIG*")
    #_(clojure.pprint/pprint *config*)
    #_(println "CONFIG.EDN")
    #_(clojure.pprint/pprint arg-map)
  (-> arg-map
      normalize-keys
      handle-verbose
      init-modules
      dispatch-command))

(defn -main [& args]
  (config/start!)
  (docopt/docopt
   usage
   args
   process-args))
