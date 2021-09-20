(ns jerzywie.csv-async-test
  (:require [jerzywie.csv :as sut]
            [jerzywie.test-util :as test-util]
            [cljs.test :refer-macros [deftest async is]]
            [cljs.core.async :as async :refer [go <!]]))

(def expected-keys '(:accinfo :txns))
(def expected-txns-count 58)
(def expected-header-keys-count 3)

(deftest file-tests-async
  (let [_ (test-util/request-input-element-value "inp")]
    (async done
           (go
             (let [raw-data (<! test-util/file-reads)
                   sd (sut/transform-raw-data raw-data)]
               (is (= (count (keys sd)) (count expected-keys)))
               (is (= (keys sd) expected-keys))
               (is (= (count (:txns sd)) expected-txns-count))
               (is (= (count (:accinfo sd)) expected-header-keys-count))
               (done))))))
