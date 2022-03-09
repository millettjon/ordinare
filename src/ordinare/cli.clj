(ns ordinare.cli
  (:require
   [babashka.fs :as fs]
   [docopt.core :as docopt]
   [ordinare.command :as command]
   [ordinare.log :as log]
   [ordinare.util :refer [flip]]))

;; Ref: http://docopt.org/
(def usage "ordinare - organize directory

Usage:
  ord [options] configure [<module> ...]
  ord status [<module> ...]

Options:
  -v --verbose
  ")

(defn normalize-key
  [s]
  (let [s (condp re-matches s
            #"<(.+)>" :>> second
            s)]
    (keyword s)))

(defn find-config-dir
  []
  (loop [dir (fs/real-path ".")]
    (let [path   (fs/path dir ".ord")
          parent (fs/parent dir)]
      (cond
        (fs/directory? path) (str path)
        parent               (recur parent)))))

(defn normalize
  [arg-map]
  (reduce-kv
   (fn [m k v]
     (assoc m (normalize-key k) v))
   {:ordinare/config-dir (find-config-dir)}
   arg-map))

(defn dispatch
  [arg-map]

  (when (:--verbose arg-map)
    (alter-var-root #'log/*level* (constantly :debug)))

  (log/debug "arguments" arg-map)

  ((condp (flip get) arg-map
     :configure command/configure
     :status    command/status)
   arg-map))

(defn -main [& args]
  (docopt/docopt
   usage
   args
   (comp dispatch normalize)))
