(ns jerzywie.ffa-accounts.report-util-test
  (:require [jerzywie.ffa-accounts.report-util :as sut]
            [cljs.test :refer [are deftest]]))


(deftest format-account-name-tests
  (are [name-set name-string] (= (sut/format-account-name name-set) name-string)
    #{"X" "Y"}            "X & Y"
    #{"Y" "X"}            "X & Y"
    #{" " "X" "Y"}        "X & Y"
    #{"Y" "X" " "}        "X & Y"
    #{"" "X" "Y"}         "X & Y"
    #{"Y" "X" ""}         "X & Y"
     #{"X"}                "X"
    #{" " "X"}            "X"
    #{"X" " "}            "X"
    #{"Z" "Y" "x" "p"}    "Y & Z & p & x"
    #{"Z & Y" "A"}        "A & Z & Y"
    ))

(deftest format-donor-amounts-tests
  (are [raw-amounts formatted-amounts] (= (sut/format-donor-amounts raw-amounts) formatted-amounts)
    (list [1 16] [3 15]) "1 x £16.00, 3 x £15.00"
    (list [3 15] [1 16]) "3 x £15.00, 1 x £16.00"
    (list [1 34.56])     "1 x £34.56"))

(deftest get-total-tests
  (are [raw-amounts total] (= (sut/get-total raw-amounts) total)
    (list [1 16] [3 15]) 61
    (list [3 15] [1 16]) 61
    (list [2 12] [5 6])  54))
