(ns jerzywie.allocate-test
  (:require [jerzywie.allocate :as sut]
            [jerzywie.test-util :as util]
            [jerzywie.csv :as csv]
            [cljs.test :refer [are deftest is testing use-fixtures]]))

(use-fixtures :each util/start-with-empty-cache)

(deftest strip-prefix-tests
  (testing "strip-prefix behaves correctly"
    (are [x y] (= (sut/strip-prefix x) y)
      "Bank credit XYZ" "XYZ"
      "Transfer from PQR" "PQR"
      "Bank creditAll of You" "All of You"
      "Transfer from      Only this" "Only this"
      "Just this bit" "Just this bit")))

(deftest make-group-tests
  (testing "make-group extracts a valid group id"
    (are [name desc result] (= (sut/make-group name desc) result)
      "Same" "Same" nil
      "This is a name" "This is the description" nil
      "AN Other" "Credit 24 May 2021" nil
      "Fred Bloggs" "123456 87654321" "123456 87654321")))

(deftest process-name-tests
  (testing "process-name handles name and description correctly"
    (are [map result] (= (sut/process-name map) result)
      {:date :adate :type "Bank credit COCTEAU TWINS" :desc "Bank credit COCTEAU TWINS"}
      {:name "COCTEAU TWINS" :group nil}

      {:type "Transfer from FRED BLOGGS" :desc "Transfer from FRED BLOGGS"}
      {:name "FRED BLOGGS" :group nil}

      {:type "Transfer from FRED BLOGGS" :desc "112233 78903456"}
      {:name "FRED BLOGGS" :group "112233 78903456"}

      {:type "Transfer from FRED BLOGGS &" :desc "Transfer from FRED BLOGGS &"}
      {:name "FRED BLOGGS" :group nil})))

(defn cache-two-unrelated []
  (sut/cache-name {:name "A" :group nil})
  (sut/cache-name {:name "B" :group nil}))

(defn cache-two-related []
  (sut/cache-name {:name "C" :group "c-group"})
  (sut/cache-name {:name "D" :group "c-group"}))

(deftest cache-name-tests
  (testing "cache-name caches correctly.1"
    (are [map func result] (= (func (sut/cache-name map)) result)
      {:name "UNA" :group nil}
      (fn [r] (-> (vals r) first :names (contains? "UNA")))
      true

      {:name "DUO" :group nil}
      (fn [r] (-> (vals r) count))
      2

      {:name "TRIO" :group "AGROUP"}
      (fn [r] (-> (vals r) count))
      3)))

(deftest cache-name-tests-unrelated-names
  (testing "cache-name caches unrelated names correctly"
    (let [cache (cache-two-unrelated)]
      (is (= (-> cache vals count) 2))
      (is (nil? (->> cache vals second :group)))
      (is (= (-> cache vals second :filterby) :names)))))

(deftest cache-name-tests-related-names
  (testing "cache-name caches related names correctly"
    (let [cache (cache-two-related)]
      (is (= (-> cache vals count) 1))
      (is (= (->> cache vals first :group) "c-group"))
      (is (= (-> cache vals first :filterby) :group))
      (is (= (-> cache vals first :names) #{"C" "D"})))))

(deftest cache-name-tests-related-and-unrelated
  (testing "more cache-name tests"
    (let [_ (cache-two-related)
          cache (cache-two-unrelated)
          cache2 (cache-two-unrelated)]
      (is (= (-> cache vals count) 3))
      (is (= (-> cache vals first :group) "c-group"))
      (is (= (-> cache vals first :filterby) :group))
      (is (= (-> cache vals first :names count) 2))
      (is (= (-> cache vals second :names count) 1))
      (is (nil? (-> cache vals second :group)))
      (is (= cache cache2)))))
