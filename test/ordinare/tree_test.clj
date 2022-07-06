(ns ordinare.tree-test
  (:require
   [clojure.test :refer [deftest are testing]]
   [ordinare.tree :as sut]))

#_ (clojure.test/run-tests)

(deftest normalize-node-test
  (testing "directory"
    (are [in out] (= out (-> in
                             (#'sut/normalize-node)
                             (dissoc :ord/name :ord/spec :ord/fn)))
      ;; if ends in / it should be a directory
      ["src/"]
      {:ord/type :directory, :path "src/"}

      ;; if it has children then it should be a directory
      ["src" ["ord"]]
      {:ord/type :directory
       :path "src"
       :ord/children [["ord"]]}))

  (testing "file"
    (are [in out] (= out (-> in
                             (#'sut/normalize-node)
                             (dissoc :ord/name :ord/spec :ord/fn)))
      ;; copy file from store
      ["foo"]
      {:ord/type :file
       :path "foo"}

      ;; copy file from string"
      ["foo" "abc"]
      {:ord/type :file
       :path "foo"
       :args ["abc"]}

      ;; copy file with path
      ["src/foo"]
      {:ord/type :file
       :path "src/foo"})))

#_(comment
    (normalize-node [{:type :foo, :fn identity} {:bar "BAR"}])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3 [:foo]])
  (normalize-node ["src/foo" {:bar "BAR"} 1 2 3 ^:ordinare/arg [:foo] [:bar]]))
