(ns jerzywie.cache-test
  (:require [jerzywie.cache :as sut]
            [jerzywie.test-util :as util]
            [cljs.test :refer-macros [deftest is use-fixtures]]))

(use-fixtures :each util/start-with-empty-cache)

(deftest cache-tests
  (is (nil? (sut/get-cache-value "nonesuch")))
  (is (nil? (sut/get-cache-keys)))
  (is (= (sut/cache! 1 {:a "A" :b "B"}) {1 {:a "A" :b "B"}}))
  (is (nil? (sut/get-cache-value 2)))
  (is (= (sut/get-cache-value 2 "blah") "blah"))
  (is (= (sut/get-cache-value 1) {:a "A" :b "B"}))
  (sut/cache! 2 {:q "Q"})
  (is (= (count (sut/get-cache-keys)) 2))
  (is (= (sut/get-cache-value 1) {:a "A" :b "B"}))
  (is (= (sut/get-cache-value 2) {:q "Q"}))
  (sut/cache! 1  {:a "AAAA" :x "XYZ"})
  (is (= (count (sut/get-cache-keys)) 2))
  (is (= (sut/get-cache-value 1 "blah") {:a "AAAA" :x "XYZ"})))
