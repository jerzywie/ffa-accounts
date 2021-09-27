(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [clojure.string :as s]))

(defn format-donor-amounts [amounts]
  (s/join ", "
        (reduce (fn [v [count amount]]
                  (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-donor-amount-total [amounts]
  (reduce (fn [v [count amount]] (+ v (* count amount))) 0 amounts))

(defn add-up [map-list amount-key]
  (->> map-list (map amount-key) (reduce +)))

(defn calc-grand-total [category-totals interval]
  "Calculate the grand total of donations per 'interval' where interval is
   one of the keys in multipliers."
  (let [multipliers {:weekly 52 :monthly 12}
        one-offs #{:one-off :new-amount}]
    (if-let [divisor  (interval multipliers)]
      (->> category-totals
           (map (fn [[freq total]] (* (freq multipliers) total)))
           (apply +)
           (#(/ % divisor)))
      (if (contains? one-offs interval)
        (interval category-totals)
        0))))

(defn get-summary-donation-totals [txns]
  (let [summ-map (->> txns
                      (group-by :freq)
                      (map (fn [[k v]] {(first k) (add-up v :in)}))
                      (apply merge))]
    (assoc summ-map
           :weekly-grand-total (calc-grand-total summ-map :weekly)
           :monthly-grand-total (calc-grand-total summ-map :monthly))))

(defn get-summary-expenditure-totals [txns]
  (let [payees (group-by (partial :desc) txns)]
    (map (fn [[k v]] [k (add-up v :out)]) payees)) )
