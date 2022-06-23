(ns ordinare.module.fs
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.fs        :as fs]
   [ordinare.effect    :as effect]))

;; TODO Move these somewhere more general?
(defn delete
  [path]
  {:fn      #(fs/delete path)
   :message (format "rm %s" path)})

(defn sym-link
  [[path target] [link-path link-target]]
  {:fn      #(fs/create-sym-link link-path link-target)
   :message (format "ln -s %s %s" target path)})

;; Note: bb does not (yet) support :alias-as in the ns form.
;; TODO create a utility macro for this?
#_(u/require ordinare.module.fs.symlink :as-alias fs.symlink)
(create-ns 'ordinare.module.fs.symlink)
(alias 'fs.symlink 'ordinare.module.fs.symlink)

(s/def ::fs.symlink/args (s/tuple string? string?))

;; TODO Refactor this out into proper symlink creator that works with relative paths in a sane way.
;; TODO There is no analogue of expand-home to simplify a path with ~.
(def symlink
  {:type :fs/symlink
   :name "symlink"
   :spec (s/keys :req-un [::fs.symlink/args])
   :fn   (fn [{[target path] :args}]
           (let [ ;; ~foo -> /home/foo
                 link-path (fs/expand-home path)

                 ;; calculate path
                 ;; work like ln if path is a directory
                 link-path (cond-> link-path
                             (fs/directory? link-path) (fs/path (fs/file-name target)))

                 ;; calculate target
                 ;; if path is relative, create a relative link (dwim)
                 link-target (cond-> target
                               (fs/relative? target) (as-> $
                                                         (fs/canonicalize $ {:nofollow-links true})
                                                         (fs/relativize (fs/parent link-path) $)))
                 ;; helper effects
                 delete-it (delete link-path)
                 create-it (sym-link [path target] [link-path link-target])]

             (cond
               ;; existing symlink
               (fs/sym-link? link-path)
               (when (not= link-target (fs/read-link link-path))
                 [delete-it create-it])

               ;; existing file
               (fs/regular-file? link-path)
               [delete-it create-it]

               ;; existing anything else
               (fs/exists? link-path)
               [(effect/warn (format "non-file exists at %s" link-path))]

               ;; doesn't exist
               (not (fs/exists? link-path))
               [create-it])))})
