(ns ordinare.module.git-project
  (:require
   [babashka.fs :as fs]
   ;; [ordinare.effect :as effect]
   ;; [ordinare.effect.fs :as e.fs]
   [ordinare.effect.git :as git]
   ;; [ordinare.log :as log]
   [ordinare.config :refer [*config*]]
   [ordinare.module :as module]
   [ordinare.util :as u]
   [clojure.spec.alpha :as s]
   [ordinare.log :as log]))

(def MODULE :git-project)

;; ----------
;; SPECS
(s/def ::url      string?)
(s/def ::dir      string?)
(s/def ::symlink  (s/tuple string? string?))
(s/def ::symlinks (s/tuple ::symlink))
(s/def ::config   (u/only-keys :req-un [::url ::dir]
                               :opt-un [::symlinks]
                               :req    [:ordinare/module]))

;; ASSERTIONS
;; - dir must be a relative path
;; - if work-dir/dir exists,
;;   - if the git url matches
;;     - ok
;;     - else warn
;; - symlink
;;   src
;;     - must be a relative path
;;     - work-dir/src base dir must exist

;; ? how to check if a git repo?
;;   git rev-parse --show-toplevel
;; ? how to check git url?
;; git config --get remote.origin.url

(defmethod module/query MODULE
  [{:keys [dir
           url]
    :as   m-conf}]
  (assert (fs/relative? dir))
  (let [{:keys [work-dir]} *config*
        target-dir         (fs/path work-dir dir)]
    (if (fs/exists? target-dir)
      (let [actual-url (git/origin-url dir)]
        (when-not (= url actual-url)
          (log/warning "git origin url mismatch"
                       {:conf       m-conf
                        :actual-url actual-url})))
      ;; TODO ? see if there is a format that can use the generic diff?
      ;;         - flatten-map treats arrays as lists
      ;; - would have to handle :url and :dir as a single effect
      {:+ [:$ "git" "clone" url dir]})))

;; TODO should this be git-clone?
;; TODO should url be repository (to match git clone help)
{:ordinare/module :git-project
 :url             "git@github.com:millettjon/ordinare.git"
 :dir             "src/ordinare2"
 :symlinks        [["bin/ord2" "bin"]]}

{:ordinare/module :git-clone
 :repostiory      "git@github.com:millettjon/ordinare.git"
 :dir             "src/ordinare2"

 ;; Should symlinks be separate?
 ;; Should there be a symlink type to test here?
 :symlinks        [["bin/ord2" "bin"]]}

;; Hiccup style
;; allows nesting of children
;; type is split out
;; allows ordering of operations
[:git-clone
 {:repostiory "git@github.com:millettjon/ordinare.git"
  :dir        "src/ordinare2"}
 [:symlink {:src "bin/ord2" :target "bin"}]]

;; ? should it be possible for nested items to work off a nested directory?
;; - simplifies config
;; - process dir could take that into account
["src" ; <-- nested directory
 {:repostiory "git@github.com:millettjon/ordinare.git"
  :dir        "ordinare2"}
 [:symlink {:src "bin/ord2" :target "../bin"}]]

;; hmm, effects are custom and not compatible with diff
;;   - could use shell fns in conf but seems too cumbersome
;; ? how does diff work in this case?

;; TODO ? add module to log context? (in wrapper method?)
;; TODO ? have all shell fns run from the work-dir?
;; - special version of $, can lookup work dir
;;   effect/process.clj ?
;; - or, always default to the global work-dir? in ordinare.process/$ ?


;; -? how to get file path?
;;    - ? can it be a full path? NO
;;      - ? force it to be relative to work-dir and see how far we get?

;; TODO clone project if missing
;; - ? what are side effects?
;;   - git clone
;;     {:+ ["git" "clone" "git@github.com:millettjon/ordinare.git" "src/ordinare2"]}
;; - ? should it handle update? NO, warn in query
;; - ? should it handle delete? NO

;; TODO create symlinks
;; - ? what are side effects?
;;   - {:+ ["ln" "src/ordinare2/bin/ord2" "bin"]}
;;   - {:+ ["bin/ord2" :-> "src/ordinare2/bin/ord2"]} ; alternate syntax
;;   -? how to check link target?
;;   -? if updating pass -f --force?
;;   - {:- [bin/ord2 :-> "xxxxx"]}} ; possible syntax
;; TODO how to test

#_(defmethod module/configure :git-project
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
