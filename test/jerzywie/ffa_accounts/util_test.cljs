(ns jerzywie.ffa-accounts.util-test
  (:require [jerzywie.ffa-accounts.util :as sut]
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

(deftest within-last-month-of-tests
  (are [reference-date test-date result] (= (sut/within-last-month-of
                                             (sut/md reference-date)
                                             (sut/md test-date))
                                            result)
    [2021 9 27] [2021 9 27] true
    [2021 9 27] [2021 9 28] false
    [2021 9 27] [2021 8 27] true
    [2021 9 27] [2021 8 26] false
    [2021 9 27] [2021 9 20] true))

(deftest last-date-in-month-tests
  (are [date result] (= (sut/last-date-in-month (sut/md date)) (sut/md result))
    [2021  1  1] [2021  1 31]
    [2021  1 10] [2021  1 31]
    [2021  1 31] [2021  1 31]
    [2021  2  1] [2021  2 28]
    [2021  2 10] [2021  2 28]
    [2021  2 28] [2021  2 28]
    [2020  2 15] [2020  2 29]
    [2021  3  3] [2021  3 31]
    [2021  4 14] [2021  4 30]
    [2021  5 15] [2021  5 31]
    [2021  6 16] [2021  6 30]
    [2021  7 17] [2021  7 31]
    [2021  8 18] [2021  8 31]
    [2021  9 19] [2021  9 30]
    [2021 10 20] [2021 10 31]
    [2021 11 21] [2021 11 30]
    [2021 12 22] [2021 12 31]))

(deftest in-same-month-as-tests
  (are [reference-date test-date result] (= (sut/in-same-month-as
                                             (sut/md reference-date)
                                             (sut/md test-date))
                                            result)
    [2021 10  5] [2021 10 31] true
    [2021 10  5] [2021 10  1] true
    [2021 10  5] [2021  9  5] false
    [2021 10  5] [2021  9 30] false
    [2021 10  5] [2021 11  1] false))
