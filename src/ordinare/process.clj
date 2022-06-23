(ns ordinare.process
  (:require
   [babashka.process :as p]
   [clojure.string :as str]
   [ordinare.fs :as fs]))

;; TODO: add verbose logging
(defn $
  [& args]
  (let [x          (first args)
        [cmd opts] (if (map? x)
                     [(rest args) x]
                     [args nil])
        opts (assoc opts :dir fs/*cwd*)]
    (-> cmd
        (p/sh opts)
        p/check
        :out
        str/split-lines)))
