(ns jerzywie.analyse
  (:require [jerzywie.cache :as nc]
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
    (let [all-but-last (take (dec (count donations-tranche)) donations-tranche)
          last-one (last donations-tranche)
          day-diff (days-between (:date last-one) analysis-date)
          add-recency (fn [max-day-diff]
                        (if (nil? max-day-diff)
                          last-one
                          (if (some #{day-diff} (range (inc max-day-diff)))
                            (assoc last-one :current true)
                            last-one)))
          freq-days (-> last-one :freq first {:weekly 7 :monthly 31})]
      (conj all-but-last (add-recency freq-days)))))

(defn analyse-donor-tranches [analysis-date tranches]
  (map #(analyse-recency analysis-date %) tranches))

(defn analyse-donations [analysis-date allocated-transactions]
  (->>
   (keys allocated-transactions)
   (map analyse-donor)
   (map #(analyse-donor-tranches analysis-date %))
   flatten
   (sort-by :date)))
