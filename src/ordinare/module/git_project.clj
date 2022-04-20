(ns ordinare.module.git-project
  (:require
   [babashka.fs :as fs]
   [ordinare.effect :as effect]
   [ordinare.effect.fs :as e.fs]
   [ordinare.log :as log]
   [ordinare.module :as module]))

#_{:ordinare/module :git-project
   :url "git@github.com:millettjon/ordinare.git"
   :dir "src/ordinare"
   :symlinks [{"bin/ord" "bin"}]}

;; TODO: handle dry run

(defmethod module/configure :git-project
  [{:keys [work-dir]
    :as   _conf}
   {:keys [dir symlinks]
    :as   _module}]
  (log/info (format "configuring directory '%s'" dir))
  (clojure.pprint/pprint _conf)
  (clojure.pprint/pprint _module)

  ;; TODO: clone git project dir if missing

  ;; Create any symlinks.
  ;;
  ;; src
  ;; - must be a relative path
  ;; - must exist (dynamic check since it may not be cloned yet)
  ;; - is relative to module dir in context
  ;; target
  ;; - is relative to work-dir
  ;; - may be a dir (if dir exists)
  (doseq [[src target] symlinks]
    (let [src (->> src (fs/path #_work-dir dir) str)
          target (->> target (fs/path #_work-dir) str)]
      (prn src target)))
  #_(e.fs/symlink src target)
  #_(-> (o.fs/symlink src target)
        effects/ensure!)

  ;; need work-dir

  ;; constructor
  ;; applied?
  ;; apply!  ; how to make dry-run generic? (around) https://github.com/camsaul/methodical
  ;; ensure! can be generic

  ;; module produces a list of effects that apply to the project
  ;; - paths are relative to project root

  ;; for purposes of effects
  ;; - should the paths be relative to the work dir?
  ;;   - likely yes since we don't want to manage files outside of our area??
  ;;   - makes it easier to work with effects
  ;;   - no need to use ~ for home dir handling since this should work w/ any dir


)
