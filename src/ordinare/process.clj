(ns ordinare.process
  (:require [babashka.process :as p]
            [clojure.string :as str]))

(defn $
  [& args]
  (-> args
      p/sh
      p/check
      :out
      str/split-lines))
