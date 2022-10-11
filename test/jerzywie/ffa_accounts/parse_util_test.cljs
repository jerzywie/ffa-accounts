(ns jerzywie.ffa-accounts.parse-util-test
  (:require [jerzywie.ffa-accounts.parse-util :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest keywordise-headers-converts-to-correct-headers
  (testing "keywordise-headers converts to correct headers."
    (is (= '(:date :type :desc :out :in :bal)
           (sut/keywordise-transaction-headers
            {:date :date
             :transactiontype :type
             :description :desc
             :paidout :out
             :paidin :in
             :balance :bal
             :accountname :account-name
             :accountbalance :account-balance
             :availablebalance :avail-balance}
            ["Date" "Transaction type" "Description" "Paid out" "Paid in" "Balance"])))))

(deftest parse-amount-tests
  (testing "parse-amount handles conversion correctly."
    (is (= 610.89 (sut/parse-amount "610.89")))
    (is (= 610.89 (sut/parse-amount "�610.89")))
    (is (= 15.00 (sut/parse-amount "£15.00")))
    (is (= 170.00 (sut/parse-amount "170")))
    (is (nil? (sut/parse-amount "")))))
