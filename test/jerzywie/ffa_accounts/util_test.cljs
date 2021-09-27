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
  (are [analysis-date other-date result] (= (sut/within-last-month-of
                                             (sut/md analysis-date)
                                             (sut/md other-date))
                                            result)
    [2021 9 27] [2021 9 27] true
    [2021 9 27] [2021 9 28] false
    [2021 9 27] [2021 8 27] true
    [2021 9 27] [2021 8 26] false
    [2021 9 27] [2021 9 20] true))
