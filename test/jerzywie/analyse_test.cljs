(ns jerzywie.analyse-test
  (:require [jerzywie.ffa-accounts.analyse :as sut]
            [jerzywie.ffa-accounts.util :refer [md]]
            [jerzywie.test-util :as test-util]
            [cljs.test :refer [are deftest is use-fixtures]]))

(def test-txns "resources/txns.edn")

(use-fixtures :each test-util/start-with-empty-cache)

(deftest deduce-period-tests
  (are [d1 d2 result] (= (sut/deduce-period (md d1) (md d2)) result)
    [2021  8  3] [2021  8 10] :weekly
    [2021  1 15] [2021  2 15] :monthly
    [2021  2 15] [2021  3 15] :monthly
    [2021  4 15] [2021  5 15] :monthly
    [2020  2 15] [2020  3 15] :monthly
    [2021  8 29] [2021  9  5] :weekly
    [2021  8  3] [2021  8  9] :approx-weekly
    [2021  8  3] [2021  8 11] :approx-weekly
    [2021  8  3] [2021  8  8] :irregular
    [2021  8  3] [2021  8 12] :irregular))

(def weekly-first-sept [{:freq #{:new-amount}} {:date (md [2021 9 1]) :freq #{:weekly}}])

(def monthly-first-sept [{:freq #{:new-amount}} {:date (md [2021 9 1]) :freq #{:monthly}}])


(deftest analyse-recency-tests
  (are
      [donations date result]
      (= result
         (contains? (first (sut/analyse-recency date donations))
                    :current))
    weekly-first-sept  (md [2021  9  8]) true
    weekly-first-sept  (md [2021  9  5]) true
    weekly-first-sept  (md [2021  9 10]) false
    weekly-first-sept  (md [2021  8 31]) false
    weekly-first-sept  (md [2021  9  1]) true
    monthly-first-sept (md [2021  9 10]) true
    monthly-first-sept (md [2021 10  1]) true
    monthly-first-sept (md [2021 10  2]) true
    monthly-first-sept (md [2021 10  3]) false
    monthly-first-sept (md [2021  8 31]) false
    monthly-first-sept (md [2021  9  1]) true))

(deftest analyse-recency-tests-one-offs
  (is (contains?
       (->
        (first (sut/analyse-recency nil [{:freq #{:new-amount}}]))
        :freq)
       :one-off)))
