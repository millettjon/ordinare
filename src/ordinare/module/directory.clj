(ns ordinare.module.directory
  (:require
   [clojure.spec.alpha  :as s]
   [ordinare.fs         :as fs]
   [ordinare.spec       :as o.s]))
#_ (remove-ns 'ordinare.module.directory)
#_ (find-ns 'ordinare.module.directory)

;; ----------
;; EFFECTS

(defn rm
  [path]
  {:fn      #(fs/delete path)
   :message "rm"})

(defn mkdir
  [path]
  {:fn       #(fs/create-dir path)
   :message "mkdir"})

;; ----------
;; MODULE

(def directory
  {:type :directory
   :spec (s/keys :req-un [::o.s/path])
   :fn   (fn
           [{:keys [path]}]
           (cond
             (fs/directory? path)
             nil

             (fs/exists? path)
             [(rm path)
              (mkdir path)]

             :else
             [(mkdir path)]))})
