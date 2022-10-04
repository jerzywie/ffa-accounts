(ns jerzywie.ffa-accounts.csv-caf
  (:require [jerzywie.ffa-accounts.util :as u]
            [jerzywie.ffa-accounts.parse-util :as pu]
            [clojure.string :as s]))

(def keyword-convert {:postingdate :date
                      :valuedate :valuedate
                      :description :desc
                      :debit :out
                      :credit :in
                      :bookbalance :bal
                      :accountname :account-name
                      :accountbalance :account-balance
                      :availablebalance :avail-balance})

(defn make-header-line-map [[k v] convert-v-to-amount?]
  {(pu/convert-to-keyword keyword-convert k) (if convert-v-to-amount? (pu/parse-amount v) v)})

(defn process-header-lines [header-lines]
  (let [header-maps (map make-header-line-map header-lines [false true true])]
    (apply merge header-maps)))

(defn format-transaction [{:keys [date valuedate type desc out in bal]} seqno]
  {:date (u/convert-txn-date-string-caf date)
   :type desc
   :desc desc
   :out (pu/parse-amount out)
   :in (pu/parse-amount in)
   :bal (pu/parse-amount bal)
   :seqno seqno})

(defn transform-raw-data [raw-data]
  (let [acc-info (process-header-lines (take 1 (drop 1 raw-data)))
        transactions (drop 9 raw-data)
        txn-headers (pu/keywordise-transaction-headers keyword-convert (first transactions))
        num-txns (- (count transactions) 1 6)
        txn-map (map #(zipmap txn-headers %) (take num-txns (rest transactions)))]
    {:accinfo acc-info
     :bank :caf
     :txns (map (fn [txn seqno] (format-transaction txn seqno)) txn-map (drop 1 (range)))}))
