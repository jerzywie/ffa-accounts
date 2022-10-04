(ns jerzywie.ffa-accounts.csv-nationwide
  (:require [jerzywie.ffa-accounts.util :as u]
            [jerzywie.ffa-accounts.parse-util :as pu]
            [clojure.string :as s]))

(def keyword-convert {:date :date
                      :transactiontype :type
                      :description :desc
                      :paidout :out
                      :paidin :in
                      :balance :bal
                      :accountname :account-name
                      :accountbalance :account-balance
                      :availablebalance :avail-balance})

(defn make-header-line-map [[k v] convert-v-to-amount?]
  {(pu/convert-to-keyword keyword-convert k) (if convert-v-to-amount? (pu/parse-amount v) v)})

(defn process-header-lines [header-lines]
  (let [header-maps (map make-header-line-map header-lines [false true true])]
    (apply merge header-maps)))

(defn format-transaction [{:keys [date type desc out in bal]} seqno]
  {:date (u/convert-txn-date-string date)
   :type type
   :desc desc
   :out (pu/parse-amount out)
   :in (pu/parse-amount in)
   :bal (pu/parse-amount bal)
   :seqno seqno})

(defn transform-raw-data [raw-data]
  (let [acc-info (process-header-lines (take 1 raw-data))
        transactions (drop 4 raw-data)
        txn-headers (pu/keywordise-transaction-headers keyword-convert (first transactions))
        txn-map (map #(zipmap txn-headers %) (rest transactions))]
    {:accinfo acc-info
     :bank :nationwide
     :txns (map (fn [txn seqno] (format-transaction txn seqno)) txn-map (drop 1 (range)))}))
