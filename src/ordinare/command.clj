(ns ordinare.command
  (:require
   [babashka.fs     :as fs]
   [clojure.string  :as str]
   [ordinare.config :as config]
   [ordinare.effect :as effect]
   [ordinare.module :as module]
   [ordinare.tree   :as tree]))

(defn display-path
  [module]
  (let [m-path (-> module :opts :path)]
    (if (-> module :context :level pos?)
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
  (display-path {:context {:level 1}, :opts {:path "foo"}})
  (display-path {:context {:level 0}, :opts {:path "."}})
  (display-path {:context {:level 0}, :opts {:path "src/ordinare"}})
  (display-path {:context {:level 0}, :opts {:path "src/ordinare/bin"}})

  ;; How to replace home with ~/
  ;; - ? manually w/ user.dir?
  (fs/canonicalize "~/src")             ; nope
  (fs/normalize "~/src")                ; nope
  (fs/expand-home "~/src")              ; works
  (fs/home)
  )

(defn- apply-effect
  [arg-map effect-fn]
  (-> arg-map
      :tree
      ;; Find any selected nodes, calculate the diff, and save any effects.
      (tree/pre-walk (fn [module]
                       (cond-> module
                         (:selected? module)
                         (assoc :effects (module/diff module)))))

      ;; Determine visible nodes to display to the user.
      (tree/post-walk (fn [module]
                        (cond-> module
                          (or (-> module :effects seq)
                              (some :visible? (:children module)))
                          (assoc :visible? true))))

      ;; Print visible nodes indented w/ user friendly string.
      (tree/pre-walk (fn [module]
                       (when (:visible? module)
                         (let [depth  (-> module :context :level)
                               indent (->> \space
                                           (repeat (* depth 2))
                                           str/join)]
                           (print indent)
                           (-> module display-path print)
                           (println "/")

                           ;; Print effects
                           (module/with-context-dir module
                             (fn [_]
                               (when-let [effects (module :effects)]
                                 (doseq [effect effects]
                                   (print indent)
                                   (print "  ")
                                   (-> module (effect/->str effect) print)
                                   (when effect-fn
                                     (let [{:keys [_ok message]} (effect-fn module effect)]
                                       (print " ")
                                       (print message)))
                                   (println)))))))
                       module))))

(defn status
  [arg-map]
  (apply-effect arg-map nil))

(defn execute
  [arg-map]
  (apply-effect arg-map effect/apply!))
