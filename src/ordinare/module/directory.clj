(ns ordinare.module.directory
  (:require
   [clojure.spec.alpha  :as s]
   [ordinare.effect     :as effect]
   [ordinare.fs         :as fs]
   [ordinare.module     :as module]
   [ordinare.spec       :as o.s :refer [only-keys]]))
#_ (remove-ns 'ordinare.module.directory)
#_ (find-ns 'ordinare.module.directory)

(def MODULE :directory)

;; ----------
;; SPECS
(s/def ::type   #(= MODULE %))
(s/def ::opts   (only-keys :req-un [::o.s/path]))
(s/def ::config (only-keys :req-un [::type ::opts]
                           :opt-un [::o.s/context
                                    ::o.s/children]))
{:type    :directory
 :opts    {:path "ordinare"}
 :context {:path  "src"
           :level 1}}

(defmethod module/diff* MODULE
  [module]
  (let [path (-> module :opts :path)]
    (cond
      (fs/directory? path) nil
      (fs/exists? path)    [[::rm path]
                            [::mkdir path]]
      :else                [[::mkdir path]])))

;; --------------------------------------------------
;; RM

(defmethod effect/->str ::rm
  [_module _effect]
  "rm")

(defmethod effect/apply*! ::rm
  [module _effect]
  (-> module :opts :path fs/delete))

;; --------------------------------------------------
;; MKDIR

(defmethod effect/->str ::mkdir
  [_module _effect]
  "mkdir")

(defmethod effect/apply*! ::mkdir
  [module _effect]
  (-> module :opts :path fs/create-dir) )
