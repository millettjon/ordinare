(ns ordinare.fs
  (:require
   [babashka.fs    :as fs]
   [clojure.string :as str])
  (:refer-clojure :exclude [empty?]))

;; TODO use babashka.fs version (w/ latest bb)
(defn cwd
  []
  (System/getProperty "user.dir"))
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

(defn apply-cwd
  [path]
  (if (fs/absolute? path)
    path
    (->> path
         (fs/path *cwd*)
         (fs/relativize (cwd)))))

(defn wrap
  [f]
  (fn [path & args]
    (apply f (apply-cwd path) args)))

;; These depend on the working directory and need path arguments adjusted.
(def canonicalize  (wrap fs/canonicalize))
(def directory?    (comp fs/directory? apply-cwd))
(def empty?        (comp empty?*       apply-cwd))
(def exists?       (comp fs/exists?    apply-cwd))
(def read-link     (comp read-link*    apply-cwd))
(def regular-file? (wrap fs/regular-file?))
(def sym-link?     (comp fs/sym-link?  apply-cwd))

;; These don't depend on the working directory so plumb straight through.
(def expand-home  fs/expand-home)
(def file-name    fs/file-name)
(def normalize    fs/normalize)
(def parent       fs/parent)
(def path         fs/path)
(def relative?    fs/relative?)
(def relativize   fs/relativize)
(def starts-with? fs/starts-with?)

;; Note: Can't call process/$ directly since process depends on this ns.
;; TODO fix
(defn $
  [& args]
  (apply (requiring-resolve 'ordinare.process/$) args))

(defn same?
  [a b]
  (try
    ($ "diff" "-q" a b)
    true
    (catch Exception ex
      (case (-> ex ex-data :exit)
        1 false ; files are different
        (throw ex)))))
#_ (same? ".gitconfig" ".gitconfig")
#_ (same? ".gitconfig" "foo")
#_ (same? ".gitconfig" "notes.org")

(defn diff
  [a b]
  (let [delta? (fs/which "delta")]
    (try
      (if delta?
        (-> ($ "delta" "--line-numbers" "--file-style=omit" "--hunk-header-style=omit" a b))
        ($ "diff" a b))
      (catch Exception ex
        (case (-> ex ex-data :exit)
          1 (-> ex ex-data :out
                ;; remove leading blank line
                (str/replace #"^\s*\n" ""))
          (throw ex))))))
#_ (diff ".gitconfig" "notes.org")
