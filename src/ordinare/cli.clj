(ns ordinare.cli
  #_(:require [docopt.core :as docopt]))

(def usage "Test application.

Usage: test-script [options]

Options:
  --an-arg <something>  An argument")
(defn -main [& args]
  (prn "-main: entering")
  (prn args)
  #_(docopt/docopt usage args
                   (fn [arg-map]
                     (println arg-map)
                     (println (arg-map "--an-arg")))))

(comment
  42
  (inc 3)
  )
