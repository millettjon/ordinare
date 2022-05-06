(ns ordinare.util-test
  (:require
   [clojure.test :refer [deftest are]]
   [ordinare.util :as sut]))

#_ (clojure.test/run-tests)

(deftest flatten-map-test
  (are [in out] (= out (sut/flatten-map in))
    {} {}

    {:user {:email "ford@prefect.com"
            :name  "Ford Prefect"}}
    {[:user :email] "ford@prefect.com"
     [:user :name] "Ford Prefect"}

    {:a {:0 "0"
         :1 "1"}
     :b "B"}
    {[:a :0] "0"
     [:a :1] "1"
     [:b] "B"}))

(deftest diff-map-test
  (are [a b out] (= out (sut/diff-map (sut/flatten-map a) (sut/flatten-map b)))
    ;; no change
    {} {} {}

    ;; simple add
    {} {:a :foo} {[:a] {:+ :foo}}

    ;; simple remove
    {:a :foo} {} {[:a] {:- :foo}}

    ;; simple update
    {:a :foo} {:a :bar} {[:a] {:- :foo
                               :+ :bar}}

    ;; unchanged value flows through
    {:a :foo} {:a :foo} {[:a] :foo}

    ;; nested; complex
    ;; before
    {:a "a-unchanged"
     :b "b-deleted"
     :c "c-changed-1"
     :e {:ea "ea-unchanged"
         :eb "eb-deleted"
         :ec "ec-changed-1"}}
    ;; after
    {:a "a-unchanged"
     :c "c-changed-2"
     :d "d-added"
     :e {:ea "ea-unchanged"
         :ed "ed-added"
         :ec "ec-changed-2"}}
    ;; result
    {[:a] "a-unchanged"
     [:b] {:- "b-deleted"}
     [:c] {:- "c-changed-1"
           :+ "c-changed-2"}
     [:d] {:+ "d-added"}
     [:e :ea] "ea-unchanged"
     [:e :eb] {:- "eb-deleted"}
     [:e :ec] {:- "ec-changed-1"
               :+ "ec-changed-2"}
     [:e :ed] {:+ "ed-added"}}))
