(ns ordinare.fs.fx
  (:require
   [babashka.fs      :as fs]
   [ordinare.fs      :as o.fs]
   [ordinare.process :refer [$]])
  (:refer-clojure :exclude [spit]))

;; These depend on the working directory and need path arguments adjusted.
(def create-dir      (comp fs/create-dir o.fs/apply-cwd))
(def create-sym-link (o.fs/wrap fs/create-sym-link))
(def delete          (comp fs/delete o.fs/apply-cwd))

;; TODO -? create directories?
(defn spit
  [path contents]
  (-> path
      o.fs/apply-cwd
      str
      (clojure.core/spit contents)))

;; These use $ which adjusts the working directory when running the command.
;; Thus there is no need to adjust the path arguments.

;; TODO -? copy symlinks as symlinks?
;; TODO -? copy directories recursively?
(defn copy
  [from to]
  ($ "cp" "-p" from to))
#_ (copy ".gitconfig" ".gitconfig.TEST")
