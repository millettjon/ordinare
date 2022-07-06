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
