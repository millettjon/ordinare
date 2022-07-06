(ns ordinare.command
  (:require
   [babashka.fs     :as fs]
   [ordinare.config :as config]
   [ordinare.module :as module]
   [ordinare.tree   :as tree]
   [ordinare.util   :as u])
  (:refer-clojure :exclude [apply]))

(defn display-path
  [module]
  (let [m-path (-> module :path)]
    (if (-> module :ord/context :level pos?)
      m-path
      (let [path (fs/path (:work-dir config/*config*)
                          m-path)
            #_ #_cwd  (o.fs/cwd)]
        ;; TODO make relative if under cwd (need fn)
        ;; TODO use ~/ if under home dir (need fn)
        #_(if (fs/starts-with? ))
        (-> path
            fs/normalize
            str)))))
(comment
  (display-path {:ord/context {:level 1}, :opts {:path "foo"}})
  (display-path {:context {:level 0}, :opts {:path "."}})
  (display-path {:context {:level 0}, :opts {:path "src/ordinare"}})
  (display-path {:context {:level 0}, :opts {:path "src/ordinare/bin"}})

  ;; How to replace home with ~/
  ;; - ? manually w/ user.dir?
  (fs/canonicalize "~/src")             ; nope
  (fs/normalize "~/src")                ; nope
  (fs/expand-home "~/src")              ; works
  (fs/home))

(defn- apply-effect
  [arg-map apply-effects?]
  (-> arg-map
      :tree

      ;; Find any selected nodes, calculate the diff, and save any effects.
      (tree/pre-walk (fn [module]
                       (cond-> module
                         (:ord/selected? module)
                         (assoc :ord/effects (module/diff module)))))

      ;; Determine visible nodes to display to the user.
      (tree/post-walk (fn [module]
                        (cond-> module
                          (or (-> module :ord/effects seq)
                              (some :ord/visible? (:ord/children module)))
                          (assoc :ord/visible? true))))

      ;; Print visible nodes indented w/ user friendly string.
      (tree/pre-walk (fn [module]
                       (when (:ord/visible? module)
                         ;; Print module header.
                         (let [depth (-> module :ord/context :level)]
                           (print (u/indent depth))
                           (case (:ord/type module)
                             :directory (do
                                          (-> module display-path print)
                                          (println "/"))
                             (println (or (when-let [name (:ord/name module)]
                                            (if (fn? name)
                                              (name module)
                                              name))
                                          (:ord/type module))))

                           ;; Print and apply effects.
                           (module/with-context-dir module
                             (fn [_]
                               (when-let [effects (module :ord/effects)]
                                 (doseq [effect effects]
                                   (u/prindent (inc depth) (:message effect "?"))
                                   (when apply-effects?
                                     (when-let [f (:fn effect)]
                                       ;; TODO add logging?
                                       (f)))))))

                           ;; Run callback
                           (when-let [on-apply (:on-apply module)]
                             (u/prindent (inc depth) "on-apply")
                             (u/prindent (+ 2 depth) (:message on-apply))
                             (when apply-effects?
                               (when-let [f (:fn on-apply)]
                                 ;; TODO add logging?
                                 (f))))))
                       module))))

;; TODO This has no way to change the directory in the shell.
;; or exec a shell in the directory
;; or run a command from the directory
#_(defn wd
  [_arg-map]
  (->> config/*config* :config-dir println))

(defn status
  [arg-map]
  (apply-effect arg-map false))

(defn apply
  [arg-map]
  (apply-effect arg-map true))
