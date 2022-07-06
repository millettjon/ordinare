(ns ordinare.module.fs
  (:require
   [clojure.spec.alpha :as s]
   [ordinare.fs        :as fs]
   [ordinare.fs.fx     :as fs.fx]
   [ordinare.effect    :as effect]
   [ordinare.spec      :as o.s]
   [ordinare.store     :as store]
   [ordinare.util      :as u]
   [selmer.parser      :as selmer])
  (:refer-clojure :exclude [spit]))

;; ----------
;; EFFECTS

(defn mkdir
  [path]
  {:fn       #(fs.fx/create-dir path)
   :message "mkdir"})

(defn spit
  [path content render?]
  {:fn      #(fs.fx/spit path content)
   :message (if render?
              "render from template"
              "copy from string")})

(defn copy
  [from to]
  {:fn      #(fs.fx/copy from to)
   :message "copy from store"})

(defn diff
  [diff-output]
  {:message (format "diff\n%s" (u/indent 1 diff-output))})

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
;; Create ns for file specs.
(create-ns 'ordinare.module.fs.file)
(alias 'fs.file 'ordinare.module.fs.file)

(s/def ::fs.file/args (s/cat :content (s/? string?)))
(s/def ::fs.file/data map?)
(s/def ::fs.file/opts (s/keys :opt-un [::fs.file/data]))

(s/def ::fs/file
  (s/keys :req-un [::o.s/path]
          :opt-un [::fs.file/args
                   ::fs.file/opts]))

(def file
  {:type :file
   :name #(:path %)
   :spec ::fs/file
   :fn   (fn [{:keys     [path data]
               [content] :args
               :as       module}]
           (let [store-path (store/resolve module)
                 new?       (not (fs/exists? path))]
             (if-not (or new? (fs/regular-file? path))
               ;; warn if not a file
               [(effect/warn (format "non-file exists at %s" path))]

               ;; create new or update existing file
               (let [;; Get source data (content or file).
                     from
                     (cond
                       content    content
                       store-path (if data
                                    (fs/slurp store-path)
                                    (fs/path store-path)))

                     ;; Render from template (if data was passed).
                     from (some-> from
                                  (cond->
                                      data (selmer/render data)))

                     ;; Calculate the diff if there is a from source.
                     changes (some->> from (fs/diff path))]

                 ;; Return the applicable effects.
                 (if (nil? from)
                   [(effect/warn "file not found in store")]
                   (cond-> []
                     ;; add the diff (if any)
                     changes (conj (diff changes))
                     ;; add side effects (if any)
                     changes (conj (if (string? from)
                                     (spit path from data)
                                     (copy from path)))))))))})

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
