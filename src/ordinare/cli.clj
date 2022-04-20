(ns ordinare.cli
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [docopt.core :as docopt]
   [ordinare.command :as command]
   [ordinare.log :as log]
   [ordinare.util :refer [flip]]))

;; Ref: http://docopt.org/
(def usage "ordinare - organize directory

Usage:
  ord [options] configure [<dir-or-module> ...]
  ord status [<dir-or-module> ...]

Options:
  -v --verbose
  -n --dry-run
  ")

(defn find-config-dir
  []
  (loop [dir (fs/real-path ".")]
    (let [path   (fs/path dir ".ord")
          parent (fs/parent dir)]
      (cond
        (fs/directory? path) (str path)
        parent               (recur parent)))))

(defn get-config
  []
  (let [dir (find-config-dir)]
    (merge
     {:config-dir dir
      :work-dir (-> dir fs/parent str)}
     (-> (fs/path dir "config.edn")
         str
         slurp
         edn/read-string))))

(defn resolve-module-list
  "resolve dir-or-module into list of modules"
  [conf arg-map]
  (let [targets (-> arg-map :dir-or-module set)]
    (-> arg-map
        (assoc :modules (->> conf
                             :modules
                             (filter (fn [module]
                                       (or (targets (:dir module))
                                           (targets (-> module :ordinare/module str)))))
                             seq))
        (dissoc :dir-or-module))))

;; TODO throw if no config dir found
;; TODO throw if no work dir found
;; TODO check paths relative to working directory
;;    - if relative path
;;      combine with current directory
;;      find path under work-dir
;;      check module list for that
;; TODO throw an exception of module fails to resolve

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
  [conf arg-map]
  (log/debug "conf" conf)
  (log/debug "arguments" arg-map)
  ((condp (flip get) arg-map
     :configure command/configure
     :status    command/status)
   conf
   arg-map))

(defn process-args
  [arg-map]
  (let [conf    (get-config)
        arg-map (-> arg-map
                    normalize-keys
                    handle-verbose
                    (->> (resolve-module-list conf)))]
    (dispatch conf arg-map)))
(defn -main [& args]
  (docopt/docopt
   usage
   args
   process-args))
