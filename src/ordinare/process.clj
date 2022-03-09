(ns ordinare.process
  (:require [babashka.process :as p]
            [clojure.string :as str]))

;; TODO: add verbose logging
(defn $
  [& args]
  (-> args
      p/sh
      p/check
      :out
      str/split-lines))
