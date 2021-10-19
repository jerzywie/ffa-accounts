(ns jerzywie.ffa-accounts.analyse-test
  (:require [jerzywie.ffa-accounts.analyse :as sut]
            [jerzywie.ffa-accounts.util :refer [md]]
            [jerzywie.ffa-accounts.test-util :as test-util]
            [cljs.test :refer [are deftest is use-fixtures]]))

(def test-txns "resources/txns.edn")

(use-fixtures :each test-util/start-with-empty-cache)

(deftest deduce-period-tests
  (are [d1 d2 result] (= (sut/deduce-period (md d1) (md d2)) result)
    [2021  8  3] [2021  8 10] {:period :weekly :freqq :regular}
    [2021  1 15] [2021  2 15] {:period :monthly :freqq :regular}
    [2021  2 15] [2021  3 15] {:period :monthly :freqq :regular}
    [2021  4 15] [2021  5 15] {:period :monthly :freqq :regular}
    [2020  2 15] [2020  3 15] {:period :monthly :freqq :regular}
    [2021  8 29] [2021  9  5] {:period :weekly :freqq :regular}
    [2021  8  3] [2021  8  9] {:period :approx-weekly :freqq :regular}
    [2021  8  3] [2021  8 11] {:period :approx-weekly :freqq :regular}
    [2021  8  3] [2021  8  8] {:period :none :freqq :irregular}
    [2021  8  3] [2021  8 12] {:period :none :freqq :irregular}))

(def weekly-first-sept [{:date (md  [2021 8 25])  :period :weekly :freqq :regular :new true}
                        {:date (md  [2021 9  1])  :period :weekly :freqq :regular}])

(def monthly-first-sept [{:date (md [2021 8  1])  :period :monthly :freqq :regular :new true}
                         {:date (md [2021 9  1])  :period :monthly :freqq :regular}])

(def august-weekly-txns [{:date (md [2021 7 25])  :period :weekly :freqq :regular :new true}
                         {:date (md [2021 8  1])  :period :weekly :freqq :regular}
                         {:date (md [2021 8  8])  :period :weekly :freqq :regular}
                         {:date (md [2021 8 15])  :period :weekly :freqq :regular}
                         {:date (md [2021 8 22])  :period :weekly :freqq :regular}
                         {:date (md [2021 8 29])  :period :weekly :freqq :regular}])

(defn current? [x] (contains? x :current))

(deftest analyse-recency-tests
  (are
      [donations analysis-date match-expected? current-date]
      (let [ard (sut/analyse-recency (md analysis-date) donations)
            current (filter current? ard)]
        (if match-expected?
          ; match date of element marked with :current true and expect only one of them
          (and (= (md current-date)
                  (:date (first current)))
               (= (count current) 1))
          ; check that no elements are marked with :current
          (= (count current) 0)))
    weekly-first-sept  [2021  9  7] true  [2021 9  1]
    weekly-first-sept  [2021  9  8] false      nil
    weekly-first-sept  [2021  9  5] true  [2021 9  1]
    weekly-first-sept  [2021  9 10] false      nil
    weekly-first-sept  [2021  8 31] true  [2021 8 25]
    weekly-first-sept  [2021  9  1] true  [2021 9  1]
    monthly-first-sept [2021  9 10] true  [2021 9  1]
    monthly-first-sept [2021 10  1] true  [2021 9  1]
    monthly-first-sept [2021 10  2] false      nil
    monthly-first-sept [2021  8 31] true  [2021 8  1]
    monthly-first-sept [2021  7 31] false      nil
    monthly-first-sept [2021  9  1] true  [2021 9  1]
    august-weekly-txns [2021  8 29] true  [2021 8 29]
    august-weekly-txns [2021  8 22] true  [2021 8 22]
    august-weekly-txns [2021  8 15] true  [2021 8 15]
    august-weekly-txns [2021  8 16] true  [2021 8 15]
    august-weekly-txns [2021  8  8] true  [2021 8  8]
    august-weekly-txns [2021  8  1] true  [2021 8  1]
    august-weekly-txns [2021  7 31] true  [2021 7 25]
    august-weekly-txns [2021  7 24] false      nil
    ))

(deftest analyse-recency-tests-one-offs
  (is (= 1
       (->
        (first (sut/analyse-recency nil [{:freq #{:new-amount}}]))
        :freq
        count))))
