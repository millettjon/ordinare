(ns ordinare.effect.ini
  (:require
   [clojure-ini.core :as ini]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske])
  (:refer-clojure :exclude [read]))

(defn read
  [file]
  (->> file
       ini/read-ini
       (cske/transform-keys csk/->kebab-case-keyword)))
