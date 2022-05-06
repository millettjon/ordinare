(ns ordinare.config
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]))

(defn find-config-dir
  []
  (loop [dir (fs/real-path ".")]
    (let [path   (fs/path dir ".ord")
          parent (fs/parent dir)]
      (cond
        (fs/directory? path) (str path)
        parent               (recur parent)))))

(defn read-config
  []
  (let [dir (find-config-dir)]
    (merge
     {:config-dir dir
      :work-dir   (-> dir fs/parent str)}
     (-> (fs/path dir "config.edn")
         str
         slurp
         edn/read-string))))

(def ^:dynamic *config*
  (read-config))

#_(with-bindings {#'*config* {:foo "FOO"}}
    *config*  )

(comment
  ;; -? does edn support meta data? yes
  (let [x (edn/read-string
           "^:ordinare.module/arg []")]
    (meta x))
  )
