(ns ordinare.module.fs-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest are]]
   [ordinare.module.fs]
   [ordinare.fs :as fs]))

#_ (clojure.test/run-tests)

(deftest spec-test
  (are [data] (s/valid? ::fs/file data)
    ;; copy file from store to path
    {:path "foo.conf"}

    ;; copy string to path
    {:path "foo.conf",
     :args ["some thing"]}

    ;; render template from store using data
    {:path "foo.conf",
     :opts {:data {:x 42}}}

    ;; render template from string using data
    {:path "foo.conf",
     :args ["some thing"]
     :opts {:data {:x 42}}}))
