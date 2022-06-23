(ns ordinare.fs
  (:require
   [babashka.fs :as fs])
  (:refer-clojure :exclude [empty?]))

;; TODO - ? denote pure vs effectful functions?
;;   - ? different ns?

;; TODO use babashka.fs version (w/ latest bb)
(defn cwd
  []
  (System/getProperty "user.dir"))
#_ (cwd)
#_ (fs/cwd)

(def ^:dynamic *cwd*
  (cwd))

(defmacro with-cwd
  [dir & body]
  `(with-bindings {#'*cwd* ~dir}
     ~@body))

;; TODO add to babashka.fs
(defn- empty?*
  [path]
  (-> path
      fs/list-dir
      clojure.core/empty?))

;; TODO add to babashka.fs
;; This works even if the link target doesn't exist.
(defn- read-link*
  [path]
  (-> path
      fs/path
      java.nio.file.Files/readSymbolicLink))

(defn- apply-cwd
  [path]
  (if (fs/absolute? path)
    path
    (->> path
         (fs/path *cwd*)
         (fs/relativize (cwd)))))

;; Wrappers

;; TODO make macro for 2 arity wrapper
(defn- wrap
  [f]
  (fn [path & args]
    (apply f (apply-cwd path) args)))

(def canonicalize (wrap fs/canonicalize))
(def create-sym-link (wrap fs/create-sym-link))
(def delete     (comp fs/delete     apply-cwd))
(def directory? (comp fs/directory? apply-cwd))
(def empty?     (comp empty?*       apply-cwd))
(def exists?    (comp fs/exists?    apply-cwd))
(def read-link  (comp read-link*    apply-cwd))
(def regular-file? (wrap fs/regular-file?))
(def sym-link?  (comp fs/sym-link?  apply-cwd))

;; TODO add 2 arity version to allow setting permissions
(def create-dir (comp fs/create-dir apply-cwd))

;; pure / pass through
(def delete       fs/delete)
(def expand-home  fs/expand-home)
(def file-name    fs/file-name)
(def normalize    fs/normalize)
(def parent       fs/parent)
(def path         fs/path)
(def relative?    fs/relative?)
(def relativize   fs/relativize)
(def starts-with? fs/starts-with?)
