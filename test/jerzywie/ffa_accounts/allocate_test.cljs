(ns jerzywie.ffa-accounts.allocate-test
  (:require [jerzywie.ffa-accounts.allocate :as sut]
            [jerzywie.ffa-accounts.test-util :as util]
            [cljs.test :refer [are deftest is testing use-fixtures]]))

(use-fixtures :each util/start-with-empty-cache)

(deftest strip-prefix-tests
  (testing "strip-prefix behaves correctly"
    (are [x y] (= (sut/strip-prefix x) y)
      "Bank credit XYZ" "XYZ"
      "Transfer from PQR" "PQR"
      "Bank creditAll of You" "All of You"
      "Transfer from      Only this" "Only this"
      "Just this bit" "Just this bit")))

(deftest make-group-tests
  (testing "make-group extracts a valid group id"
    (are [name desc result] (= (sut/make-group name desc) result)
      "Same" "Same" nil
      "This is a name" "This is the description" nil
      "AN Other" "Credit 24 May 2021" nil
      "Fred Bloggs" "123456 87654321" "123456 87654321")))

(deftest process-name-tests
  (testing "process-name handles name and description correctly"
    (are [map result] (= (sut/process-name map) result)
      {:date :adate :type "Bank credit COCTEAU TWINS" :desc "Bank credit COCTEAU TWINS"}
      {:name "COCTEAU TWINS" :group nil}

      {:type "Transfer from FRED BLOGGS" :desc "Transfer from FRED BLOGGS"}
      {:name "FRED BLOGGS" :group nil}

      {:type "Transfer from FRED BLOGGS" :desc "112233 78903456"}
      {:name "FRED BLOGGS" :group "112233 78903456"}

      {:type "Transfer from FRED BLOGGS &" :desc "Transfer from FRED BLOGGS &"}
      {:name "FRED BLOGGS" :group nil})))

(defn cache-two-unrelated []
  (sut/cache-name {:name "A" :group nil})
  (sut/cache-name {:name "B" :group nil}))

(defn cache-two-related []
  (sut/cache-name {:name "C" :group "c-group"})
  (sut/cache-name {:name "D" :group "c-group"}))

(deftest cache-name-tests
  (testing "cache-name caches correctly.1"
    (are [map func result] (= (func (sut/cache-name map)) result)
      {:name "UNA" :group nil}
      (fn [r] (-> (vals r) first :names (contains? "UNA")))
      true

      {:name "DUO" :group nil}
      (fn [r] (-> (vals r) count))
      2

      {:name "TRIO" :group "AGROUP"}
      (fn [r] (-> (vals r) count))
      3)))

(deftest cache-name-tests-unrelated-names
  (testing "cache-name caches unrelated names correctly"
    (let [cache (cache-two-unrelated)]
      (is (= (-> cache vals count) 2))
      (is (nil? (->> cache vals second :group)))
      (is (= (-> cache vals second :filterby) :names)))))

(deftest cache-name-tests-related-names
  (testing "cache-name caches related names correctly"
    (let [cache (cache-two-related)]
      (is (= (-> cache vals count) 1))
      (is (= (->> cache vals first :group) "c-group"))
      (is (= (-> cache vals first :filterby) :group))
      (is (= (-> cache vals first :names) #{"C" "D"})))))

(deftest cache-name-tests-related-and-unrelated
  (testing "more cache-name tests"
    (let [_ (cache-two-related)
          cache (cache-two-unrelated)
          cache2 (cache-two-unrelated)]
      (is (= (-> cache vals count) 3))
      (is (= (-> cache vals first :group) "c-group"))
      (is (= (-> cache vals first :filterby) :group))
      (is (= (-> cache vals first :names count) 2))
      (is (= (-> cache vals second :names count) 1))
      (is (nil? (-> cache vals second :group)))
      (is (= cache cache2)))))

(deftest process-expenditure-tests
  (let [coalesce-result       "ABC-generic"
        in-set                "ABC-first"
        also-in-set           "ABC-second"
        coalesce-set          #{in-set also-in-set}
        not-in-set            "nonesuch"
        coalesce-other-result "XYZ-result"
        in-other-set          "XYZ-name1"
        also-in-other-set     "XYZ-name2"
        coalesce-other-set    #{in-other-set also-in-other-set}
        not-in-other-set      "XYZ-not"
        coalesce-map          {coalesce-result coalesce-set
                               coalesce-other-result coalesce-other-set}
        so                    "Standing order"
        payment               "Payment"
        pt                    (str payment " to")
        txn-list              (list {:desc not-in-set        :seqno 1 :out 10 :type so}
                                    {:desc in-set            :seqno 2 :out 20 :type pt}
                                    {:desc also-in-set       :seqno 3 :out 30 :type pt}
                                    {:desc not-in-other-set  :seqno 4 :out 40 :type pt}
                                    {:desc "not-payment-1"   :seqno 5 :in 100 :type "_"}
                                    {:desc in-other-set      :seqno 6 :out 50 :type so}
                                    {:desc also-in-other-set :seqno 7 :out 60 :type pt}
                                    {:desc "not payment-2"   :seqno 8 :in 100 :type "_"})]
    (testing "coalesce-one-payee"
      (is (= coalesce-result
             (sut/coalesce-one-payee in-set coalesce-set coalesce-result)))

      (is (= coalesce-other-result
             (sut/coalesce-one-payee in-other-set coalesce-other-set coalesce-other-result)))

      (is (nil? (sut/coalesce-one-payee not-in-set coalesce-set coalesce-result)))

      (is (nil? (sut/coalesce-one-payee
                 not-in-other-set
                 coalesce-other-set
                 coalesce-other-result))))

    (testing "coalesce-payees"
      (is (= {:desc not-in-set :seqno 999}
             (sut/coalesce-payees coalesce-map {:desc not-in-set :seqno 999})))

      (is (= {:desc coalesce-result :seqno 999}
             (sut/coalesce-payees coalesce-map {:desc in-set :seqno 999}))))

    (let [exp-list (->> txn-list
                        (sut/process-expenditure coalesce-map)
                        (sort-by :seqno))]

      (testing "prettify-payment type"
        (is (= so      (:type (first exp-list))))
        (is (= payment (:type (second exp-list)))))

      (testing "process-expenditure"
        (is (= 6                     (count exp-list)))
        (is (= 7                     (-> exp-list last :seqno)))
        (is (= not-in-set            (-> exp-list first :desc)))
        (is (= coalesce-result       (-> exp-list second :desc)))
        (is (= coalesce-other-result (-> exp-list (nth 4) :desc)))
        (is (= coalesce-other-result (-> exp-list (nth 5) :desc)))))))
