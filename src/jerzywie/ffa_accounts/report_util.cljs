(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [clojure.string :as s]))

(defn format-donor-amounts [amounts]
  (s/join ", "
          (reduce (fn [v [count amount]]
                    (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-donor-amount-total [amounts]
  (reduce (fn [v [count amount]]
            (+ v (* count amount))) 0 amounts))

(defn add-up [map-list amount-key]
  (->> map-list (map amount-key) (reduce +)))

(defn calc-grand-total
  "Calculate the grand total of donations per 'interval' where interval is
   one of the keys in multipliers."
  [category-totals interval]

  (let [multipliers {:weekly 52 :fortnightly 26 :monthly 12}
        one-offs #{:one-off :new-amount}]
    (if-let [divisor  (interval multipliers)]
      (->> category-totals
           (map (fn [{:keys [name amount]}] (* (name multipliers) amount)))
           (apply +)
           (#(/ % divisor)))
      (if (contains? one-offs interval)
        (->> category-totals (filter #(= (:name %) interval)) first :amount)
        0))))

(defn calc-weekly-aggregate
  "Calculate the aggregate weekly income given a month's worth of transactions."
  [txns]
  (-> txns (add-up :in) (* 12) (/ 52)))

(defn get-summary-donation-totals
  "Return a list of maps, one for each different category."
  [txns]
  (let [summ-donations (->> txns
                            (group-by :freq)
                            (map (fn [[k v]] {:name (first k)
                                             :amount (add-up v :in)})))]
    (conj summ-donations
          {:name :weekly-grand-total
           :amount (calc-grand-total summ-donations :weekly)}
          {:name :monthly-grand-total
           :amount (calc-grand-total summ-donations :monthly)}
          {:name :weekly-aggregate
           :amount (calc-weekly-aggregate txns)})))

(defn get-summary-expenditure-totals
  "Return a list of maps, one for each different category."
  [txns]
  (let [summ-exp (->> txns
                      (group-by (partial :desc))
                      (map (fn [[k v]] {:name k :amount (add-up v :out)})))]
    (conj summ-exp
          {:name "Total expenditure for month"
           :amount (add-up summ-exp :amount)
           :is-total? true})))

(defn summary-totals->array [summary-totals-map-list]
  (->> summary-totals-map-list
       (map (fn [{:keys [name amount is-total?]}]
              (when-not is-total? [name amount])))
       (filter identity)
       (#(conj % ["Name" "Amount"]))
       (into [])))
