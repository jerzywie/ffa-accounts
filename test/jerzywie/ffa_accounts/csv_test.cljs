(ns jerzywie.ffa-accounts.csv-test
  (:require [jerzywie.ffa-accounts.csv-nationwide :as sut]
            [jerzywie.ffa-accounts.util :as util]
            [jerzywie.ffa-accounts.parse-util :as pu]
            [cljs.test :refer-macros [deftest is testing]]))

(def expected-accname "accname")
(def expected-balance {:s "�123.45" :v 123.45})
(def expected-avail-balance {:s "�98.76" :v 98.76})

(deftest keywordise-headers-converts-to-correct-headers
  (testing "keywordise-headers converts to correct headers."
    (is (= '(:date :type :desc :out :in :bal)
           (pu/keywordise-transaction-headers
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
    (is (= 610.89 (pu/parse-amount "610.89")))
    (is (= 610.89 (pu/parse-amount "�610.89")))
    (is (= 15.00 (pu/parse-amount "£15.00")))
    (is (= 170.00 (pu/parse-amount "170")))
    (is (nil? (pu/parse-amount "")))))

(def header-lines (list ["Account Name:" (:s expected-accname)]
                        ["Account Balance:" (:s expected-balance)]
                        ["Available Balance: " (:s expected-avail-balance)]))

(deftest process-header-tests
  (let [header (sut/process-header-lines header-lines)]
    (is (= (count (keys header)) 3))
    (is (= (:account-name header) (:v expected-accname)))
    (is (= (:account-balance header) (:v expected-balance)))
    (is (= (:avail-balance header) (:v expected-avail-balance)))))

(deftest format-transaction-tests
  (testing "format-transaction handles transformations correctly."
    (let [raw-transaction {:date "12 May 2021", :type "Bank credit Billy Holiday", :desc "Bank credit Billy Holiday", :out "", :in "�100.00", :bal "�640.56"}
          fmt-transaction (sut/format-transaction raw-transaction 42)
          expected {:date (util/md [2021 05 12]) :type "Bank credit Billy Holiday", :desc "Bank credit Billy Holiday", :out nil, :in 100.00, :bal 640.56 :seqno 42}]
      (is (= fmt-transaction expected)))))
