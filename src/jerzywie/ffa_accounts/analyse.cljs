(ns jerzywie.ffa-accounts.analyse
  (:require [jerzywie.ffa-accounts.cache :as nc]
            [jerzywie.ffa-accounts.util :as util]))

(defn between?
  "True if low <= value <= high."
  [value low high]
  (and (> value (dec low)) (< value (inc high))))

(defn deduce-period [d1 d2]
  (let [days (util/days-between d1 d2)]
    (cond
      (between? days 6 8)            {:period :weekly        :freq :regular}
      (between? days 12 16)          {:period :fortnightly   :freq :regular}
      (between? days 26 34)          {:period :monthly       :freq :regular}
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
                          (between? (util/days-between d1 d2) 0 31))
          grouped-donations (group-by #(within-month? (:date %) analysis-date)
                                      donations-tranche)
          add-recency (fn [{:keys [date period] :as txn}]
                        (let [day-diff (util/days-between date analysis-date)
                              max-day-diff (period
                                            {:weekly 7 :fortnightly 14 :monthly 31})]
                          (if (some #{day-diff} (range max-day-diff))
                            (assoc txn :current true)
                            txn)))
          within-month+recency (map add-recency (get grouped-donations true))
          ensure-1-current (fn [{:keys [current] :as txn} index]
                                    (let [max-index (dec (count within-month+recency))]
                                      (if (and current (not= index max-index))
                                        (dissoc txn :current)
                                        txn)))
          within-month-singular (map ensure-1-current within-month+recency (range))]
      (concat (get grouped-donations false) within-month-singular))))

(defn analyse-donor-tranches [analysis-date tranches]
  (map #(analyse-recency analysis-date %) tranches))

(defn analyse-donations [analysis-date allocated-transactions]
  (->>
   (keys allocated-transactions)
   (map analyse-donor)
   (map #(analyse-donor-tranches analysis-date %))
   flatten
   (sort-by :date)))
