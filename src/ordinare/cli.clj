(ns ordinare.cli
  (:require
   [babashka.fs      :as fs]
   [docopt.core      :as docopt]
   [ordinare.command :as command]
   [ordinare.config  :refer [*config*]]
   [ordinare.log     :as log]
   [ordinare.tree    :as tree]))

;; Ref: http://docopt.org/
(def usage "ordinare - organize directory

Usage:
  ord [options] configure [<path-or-module> ...]
  ord [options] status    [<path-or-module> ...]

Options:
  -v --verbose
  -n --dry-run
  ")

(defn path-matches?
  [module path]
  (let [cp        (-> module :context :path)
        mp        (-> module :opts :path)
        node-path (cond
                    (and cp mp) (fs/path cp mp)
                    cp          (fs/path cp)
                    :else       (fs/path mp))
        node-path (fs/normalize node-path)]
    (fs/starts-with? node-path path)))

(defn resolve-module-list
  "resolve path-or-module into list of modules"
  [arg-map]
  (let [targets (-> arg-map :path-or-module set)]
    (-> arg-map
        (assoc :modules (->> *config*
                             :root
                             (tree/module-seq {})
                             (filter (fn [module]
                                       (or (empty? targets) ; match everything if no targets
                                           (some (partial path-matches? module) targets)
                                           (targets (-> module :type name)))))
                             seq))
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

(defn dispatch
  [arg-map]
  (log/debug "arguments" arg-map)
  ((condp arg-map false
     :status    command/status
     :configure command/configure)
   arg-map))

(defn process-args
  [arg-map]
  (let [arg-map (-> arg-map
                    normalize-keys
                    handle-verbose
                    (->> (resolve-module-list)))]
    (dispatch arg-map)))

(defn -main [& args]
  (docopt/docopt
   usage
   args
   process-args))
