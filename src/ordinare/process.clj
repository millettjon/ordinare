(ns ordinare.process
  (:require [babashka.process :as p]
            [clojure.string :as str]))

;; TODO: add verbose logging
(defn $
  [& args]
  (let [x          (first args)
        [cmd opts] (if (map? x)
                     [(rest args) x]
                     [args nil])]
    (-> cmd
        (p/sh opts)
        p/check
        :out
        str/split-lines)))
