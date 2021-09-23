(ns jerzywie.ffa-accounts.analyse
  (:require [jerzywie.ffa-accounts.cache :as nc]
            [java.time.temporal :as jt]))

;(defn days-between [d1 d2]
;  (.. java.time.temporal.ChronoUnit/DAYS (between d1 d2)))

(defn days-between [d1 d2]
  (.until d1 d2 (.. jt/ChronoUnit -DAYS)))

(defn deduce-period [d1 d2]
  (let [days (days-between d1 d2)]
    (cond
      (= days 7) :weekly
      (or (= days 6) (= days 8)) :approx-weekly
      (and (> days 27) (< days 32)) :monthly
      :else :irregular)))

(defn interval-analysis [date-etc txn]
  (let [date (:date date-etc)
        results (get date-etc :results [])
        next-date (:date txn)]
    (cond
      (nil? date)
      {:date next-date
       :results (conj results (assoc txn :freq #{:new-amount}))}

      :else
      {:date next-date
       :results (conj results (assoc txn :freq  #{(deduce-period date next-date)}))})))

(defn analyse-time-intervals [txns amount]
  (let [donations (filter (fn [{:keys [in]}] (= in amount)) txns)]
    (->> donations
         (reduce interval-analysis nil)
         :results)))

(defn analyse-donor [key]
  (let [entity (nc/get-cache-value key)
        txns (:txns entity)
        amount-cache (reduce (fn [amounts {:keys [in]}] (conj amounts in)) #{} txns)]
    (map (fn [amount] (analyse-time-intervals txns amount)) amount-cache)))

(defn analyse-recency [analysis-date donations-tranche]
  (if (= 1 (count donations-tranche))
    (list (update (first donations-tranche) :freq conj :one-off))
    (let [within-month? (fn [d1 d2]
                          (let [dd (days-between d1 d2)] (and (> dd -1) (< dd 32))))
          grouped-donations (group-by #(within-month? (:date %) analysis-date)
                                      donations-tranche)
          add-recency (fn [{:keys [date freq] :as txn}]
                        (let [day-diff (days-between date analysis-date)
                              max-day-diff ((first freq) {:weekly 7 :monthly 31})]
                          (if (some #{day-diff} (range max-day-diff))
                            (assoc txn :current true)
                            txn)))
          within-month-with-recency (map add-recency (get grouped-donations true))]
      (concat (get grouped-donations false) within-month-with-recency))))

(defn analyse-donor-tranches [analysis-date tranches]
  (map #(analyse-recency analysis-date %) tranches))

(defn analyse-donations [analysis-date allocated-transactions]
  (->>
   (keys allocated-transactions)
   (map analyse-donor)
   (map #(analyse-donor-tranches analysis-date %))
   flatten
   (sort-by :date)))
