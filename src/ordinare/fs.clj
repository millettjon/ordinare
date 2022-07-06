(ns ordinare.fs
  (:require
   [babashka.fs    :as fs]
   [clojure.string :as str])
  (:refer-clojure :exclude [empty? slurp]))

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

(defn slurp
  [path]
  (-> path
      apply-cwd
      str
      clojure.core/slurp))

;; These don't depend on the working directory so plumb straight through.
(def expand-home  fs/expand-home)
(def file-name    fs/file-name)
(def normalize    fs/normalize)
(def parent       fs/parent)
(def path         fs/path)
(def relative?    fs/relative?)
(def relativize   fs/relativize)
(def starts-with? fs/starts-with?)

;; Things that call $ don't need the working directory adjusted since $ does it.

;; Note: Can't call process/$ directly since process depends on this ns.
;; TODO fix
(defn $
  [& args]
  (apply (requiring-resolve 'ordinare.process/$) args))

;; Compares a to b.
;; - b can be either a String to pass on stdin
;;   or a UnixPath to pass as a file argument
;; - Returns nil if they match.
;; TODO: make this better and able to handle - in either argument
(defn diff
  [a b]
  (let [delta? (fs/which "delta")
        args (if delta?
               ["delta" "--line-numbers" "--file-style=omit" "--hunk-header-style=omit"]
               ["diff"])

        ;; If a doesn't exist, compare against /dev/null
        a (if (exists? a) a "/dev/null")

        ;; Not sure why pr-str is needed here
        args   (case (-> b type pr-str)
                 "java.lang.String"    (concat [{:in b}] args [a "-"])
                 "sun.nio.fs.UnixPath" (concat           args [a b]))]
    (try
      (apply $ args)
      ;; return nil if files are the same
      nil
      (catch Exception ex
        (case (-> ex ex-data :exit)
          1 (-> ex ex-data :out
                ;; remove leading blank line
                (str/replace #"^\s*\n" ""))
          (throw ex))))))
