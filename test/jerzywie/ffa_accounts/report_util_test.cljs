(ns jerzywie.ffa-accounts.report-util-test
  (:require [jerzywie.ffa-accounts.report-util :as sut]
            [jerzywie.ffa-accounts.util :as util]
            [cljs.test :refer [is are deftest]]))


(deftest format-donor-amounts-tests
  (are [raw-amounts formatted-amounts] (= (sut/format-donor-amounts raw-amounts) formatted-amounts)
    (list [1 16] [3 15]) "1 x £16.00, 3 x £15.00"
    (list [3 15] [1 16]) "3 x £15.00, 1 x £16.00"
    (list [1 34.56])     "1 x £34.56"))

(deftest get-donor-amount-total-tests
  (are [raw-amounts total] (= (sut/get-donor-amount-total raw-amounts) total)
    (list [1 16] [3 15]) 61
    (list [3 15] [1 16]) 61
    (list [2 12] [5 6])  54))

(deftest add-up-tests
  (let [map-list (list {:amt 10 :xyz "a"} {:amt 25 :other 5})]
    (is (= (sut/add-up map-list :amt) 35) "Adding up list of two.")
    (is (= (sut/add-up (conj map-list {:no-amt 1}) :amt) 35) "Maps without relevant key are ignored.")))

(deftest calc-grand-total-tests
  (let [category-totals [{:name :weekly :amount 10}
                         {:name :monthly :amount 10}]
        cat-tots-with-one-offs (conj category-totals
                                     {:name :one-off :amount 80}
                                     {:name :new-amount :amount 99})]
    (is (= (util/tonumber (sut/calc-grand-total category-totals :weekly))
           (util/tonumber 12.31)))
    (is (= (util/tonumber (sut/calc-grand-total category-totals :monthly))
           (util/tonumber 53.33)))
    (is (= (util/tonumber (sut/calc-grand-total
                           cat-tots-with-one-offs :monthly))
           (util/tonumber 53.33)) "One-off amounts are ignored when periodic interval is supplied.")
    (is (= (util/tonumber (sut/calc-grand-total
                           cat-tots-with-one-offs :one-off))
           (util/tonumber 80.00)) "Periodic amounts are ignored when one-off/new-amount 'interval' is supplied.")
    (is (= (util/tonumber (sut/calc-grand-total
                           cat-tots-with-one-offs :new-amount))
           (util/tonumber 99.00)) "Periodic amounts are ignored when one-off/new-amount 'interval' is supplied.")
    (is (= (util/tonumber (sut/calc-grand-total
                           cat-tots-with-one-offs :no-such-interval))
           (util/tonumber 00.00)) "Zero is returned if interval key is not matched.")))
