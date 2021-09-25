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
