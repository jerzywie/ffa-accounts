(ns jerzywie.ffa-accounts-test
    (:require
     [cljs.test :refer-macros [deftest is testing]]
     [jerzywie.ffa-accounts :refer [multiply]]))

(deftest multiply-test
  (is (= (* 1 2) (multiply 1 2))))

(deftest multiply-test-2
  (is (= (* 75 10) (multiply 10 75))))
