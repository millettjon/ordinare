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

(defn- apply-cwd
  [path]
  (if (fs/absolute? path)
    path
    (->> path
         (fs/path *cwd*)
         (fs/relativize (cwd)))))

;; Wrappers
(def delete     (comp fs/delete     apply-cwd))
(def directory? (comp fs/directory? apply-cwd))
(def exists?    (comp fs/exists?    apply-cwd))
(def empty?     (comp empty?*       apply-cwd))

;; TODO add 2 arity version to allow setting permissions
(def create-dir (comp fs/create-dir apply-cwd))

;; pure / pass through
(def path         fs/path)
(def normalize    fs/normalize)
(def starts-with? fs/starts-with?)
