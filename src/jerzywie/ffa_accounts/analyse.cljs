(ns jerzywie.ffa-accounts.analyse
  (:require [jerzywie.ffa-accounts.cache :as nc]
            [jerzywie.ffa-accounts.util :as util]))

(defn deduce-period [d1 d2]
  (let [days (util/days-between d1 d2)]
    (cond
      (= days 7)                    {:period :weekly        :freq :regular}
      (or (= days 6) (= days 8))    {:period :approx-weekly :freq :regular}
      (= days 14)                   {:period :fortnightly   :freq :regular}
      (and (> days 27) (< days 32)) {:period :monthly       :freq :regular}
     :else                          {:period :none          :freq :irregular})))

(defn interval-analysis
  "Reduce-fn for analyse-time-intervals."
  [date-etc txn]
  (let [date (:date date-etc)
        results (get date-etc :results [])
        next-date (:date txn)]
    (cond
      (nil? date)
      {:date next-date
       :results (conj results (assoc txn :new true))}

      :else
      {:date next-date
       :results (conj results (merge txn (deduce-period date next-date)))})))

(defn analyse-time-intervals [txns amount]
  (let [donations (filter (fn [{:keys [in]}] (= in amount)) txns)]
    (->> donations
         (reduce interval-analysis nil)
         :results)))

(defn analyse-donor [key]
  (let [entity (nc/get-cache-value key)
        txns (:txns entity)
        amount-cache (reduce (fn [amounts {:keys [in]}] (conj amounts in)) #{} txns)
        donor-tranches (map (fn [amount] (analyse-time-intervals txns amount)) amount-cache)]
    (map (fn [donations-tranche]
           (if (= 1 (count donations-tranche))
             (list (assoc (first donations-tranche) :period :none :freq  :one-off))
             (let [period (-> donations-tranche second :period)
                   freq  (-> donations-tranche second :freq)]
               (conj (rest donations-tranche) (assoc (first donations-tranche) :period period :freq freq)))))
         donor-tranches)))

(defn analyse-recency [analysis-date donations-tranche]
  (if (= 1 (count donations-tranche))
    donations-tranche
    (let [within-month? (fn [d1 d2]
                          (let [dd (util/days-between d1 d2)] (and (> dd -1) (< dd 32))))
          grouped-donations (group-by #(within-month? (:date %) analysis-date)
                                      donations-tranche)
          add-recency (fn [{:keys [date period] :as txn}]
                        (let [day-diff (util/days-between date analysis-date)
                              max-day-diff (period
                                            {:weekly 7 :fortnightly 14 :monthly 31})]
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
