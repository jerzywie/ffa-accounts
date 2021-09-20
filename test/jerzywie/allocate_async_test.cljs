(ns jerzywie.allocate-async-test
  (:require [jerzywie.ffa-accounts.allocate :as sut]
            [jerzywie.test-util :as test-util]
            [jerzywie.ffa-accounts.csv :as csv]
            [cljs.test :refer [are deftest is testing use-fixtures async]]
            [cljs.core.async :as async :refer [go <!]]))

(use-fixtures :each
  {:before
   #(async done
           test-util/start-with-empty-cache
           (done))})

(def BD "BOB DYLAN")
(def SO "SONNY")
(def CH "CHER")

(deftest process-income-tests
  (let [_ (test-util/request-input-element-value "inp")]
    (async done
           (go
             (let [income-tx (-> (<! test-util/file-reads)
                                 csv/transform-raw-data
                                 :txns
                                 sut/process-income
                                 vals)
                   bd-tx (first income-tx)
                   sc-tx (nth income-tx 2)]
               (is (= (count income-tx) 6) "Check total donors")
               (is (= (count (:txns bd-tx)) 6) "Check total BD transactions.")
               (is (= (first (:names bd-tx)) BD) "Check account name.")
               (is (= (:names bd-tx) #{BD}) "Check account name.")
               (is (= (-> bd-tx :txns first :account-name first) BD) "Check account-name is propagated to transactions.")
               (is (= (-> bd-tx :txns first :name) BD) "Check it matches the name.")
               (is (= (:names sc-tx) #{CH SO}) "Check S+C account-name.")
               (is (= (:names sc-tx) (-> sc-tx :txns first :account-name)) "Check propagation of S+C account name.")
               (is (= (count (filter #(= (:name %) CH) (:txns sc-tx))) 16) "Check number of CH txns.")
               (is (= (count (filter #(= (:name %) SO) (:txns sc-tx))) 4) "Check number of SO txns.")
               (is (= (:filterby sc-tx) :group) "Check that this is a 'group' account.")
               (done))))))
