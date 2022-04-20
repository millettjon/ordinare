(ns ordinare.effect
  )

;; applied?
;; apply!
;; ensure   ; applies if not already applied
;; diff     ; filters a list of effects by applied?

;; -? add dry run flag?
;; -? how would dry run be supported?
;;    - effects/apply! is a no-op that just prints what it would do?

;; - ? how to do things :around multi-methods?
;;   -? can before be done in the dispatch?
;;   -? can just make a wrapper fn
;; effect/apply
;; effect/apply-impl
