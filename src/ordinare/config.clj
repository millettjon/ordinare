(ns ordinare.config
  (:require
   [babashka.fs :as fs]))

(defn find-config-dir
  []
  (loop [dir (fs/real-path ".")]
    (let [path   (fs/path dir ".ord")
          parent (fs/parent dir)]
      (cond
        (fs/directory? path) (str path)
        parent               (recur parent)))))

;; TODO: add .ord/src to the classpath if present?
;;   -? can bb dynamically add a dir to the classpath?
;;     yes: https://book.babashka.org/#babashka_classpath
(defn read-config
  []
  (let [dir (find-config-dir)]
    (merge
     {:config-dir dir
      :work-dir   (-> dir fs/parent str)}
     ;; TODO ? how to validate?
     ;; ? at least double check it is a map?
     (-> (fs/path dir "config.clj")
         str
         load-file))))

(def ^:dynamic *config* nil)

(defn start!
  []
  (alter-var-root #'*config* (constantly (read-config))))
#_ (start!)
