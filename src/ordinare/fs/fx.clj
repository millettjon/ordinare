(ns ordinare.fs.fx
    (:require
   [babashka.fs      :as fs]
   [ordinare.fs      :as o.fs]
   [ordinare.process :refer [$]]))

;; These depend on the working directory and need path arguments adjusted.
(def create-dir      (comp fs/create-dir o.fs/apply-cwd))
(def create-sym-link (o.fs/wrap fs/create-sym-link))
(def delete          (comp fs/delete o.fs/apply-cwd))

;; TODO ? should this create directories?
(defn touch
  [path]
  ($ "touch" path))
#_ (touch "foo/bar")

;; TODO -? copy symlinks as symlinks?
;; TODO -? copy directories recursively?
(defn copy
  [from to]
  ($ "cp" "-p" from to))
#_ (copy ".gitconfig" ".gitconfig.TEST")
