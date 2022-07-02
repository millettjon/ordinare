(ns ordinare.module.fs
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.fs        :as fs]
   [ordinare.fs.fx     :as fs.fx]
   [ordinare.effect    :as effect]
   [ordinare.spec      :as o.s]
   [ordinare.store     :as store]
   [ordinare.util      :as u]))

;; ----------
;; EFFECTS

(defn mkdir
  [path]
  {:fn       #(fs.fx/create-dir path)
   :message "mkdir"})

(defn touch
  [path]
  {:fn      #(fs.fx/touch path)
   :message "touch"})

(defn copy
  [from to]
  {:fn      #(fs.fx/copy from to)
   :message "copy from store"})

(defn diff
  [a b]
  {:message (format "diff\n%s"
                    (u/indent 1 (fs/diff a b)))})

(defn delete
  [path]
  {:fn      #(fs.fx/delete path)
   :message "rm"})

(defn sym-link
  [[path target] [link-path link-target]]
  {:fn      #(fs.fx/create-sym-link link-path link-target)
   :message (format "ln -s %s %s" target path)})

;; ----------
;; DIRECTORY MODULE

(def directory
  {:type :directory
   :spec (s/keys :req-un [::o.s/path])
   :fn   (fn
           [{:keys [path]}]
           (cond
             (fs/directory? path)
             nil

             (fs/exists? path)
             [(delete path)
              (mkdir path)]

             :else
             [(mkdir path)]))})

;; ----------
;; FILE MODULE
;; TODO render template if :render is passed
(def file
  {:type :file
   :name #(:path %)
   :spec (s/keys :req-un [::o.s/path])
   :fn   (fn [{:keys [path]}]
           (let [store-path (store/resolve path)]
             (cond
               ;; doesn't exist
               ;; copy from store or create empty file
               (not (fs/exists? path))
               (if (fs/exists? store-path)
                 [(copy store-path path)]
                 [(touch path)])

               ;; exists
               ;; copy from store
               (fs/regular-file? path)
               (if (fs/exists? store-path)
                 (when-not (fs/same? store-path path)
                   [(diff path store-path)
                    (copy store-path path)])
                 [(effect/warn "file not found in store")])

               :else
               [(effect/warn (format "non-file exists at %s" path))])))})

;; ----------
;; SYMLINK MODULE

;; Create ns for symlink specs.
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
