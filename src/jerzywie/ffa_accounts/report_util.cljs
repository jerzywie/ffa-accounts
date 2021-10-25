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
  [months-txns]
  (-> months-txns (add-up :in) (* 12) (/ 52)))

(defn get-summary-donation-totals
  "Return a list of maps, one for each different category."
  [txns]
  (let [summ-donations (->> txns
                            (group-by :period)
                            (map (fn [[k v]] {:name k
                                             :amount (add-up v :in)})))]
    summ-donations
;    (conj summ-donations
;          {:name :weekly-grand-total
;           :amount (calc-grand-total summ-donations :weekly)}
;          {:name :monthly-grand-total
;    :amount (calc-grand-total summ-donations :monthly)})
))

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

(defn monthly-txn-summary [income expend date-first-txn date-last-txn]
  (let [first-month (-> date-first-txn (.withDayOfMonth 1))
        last-month  (-> date-last-txn (.withDayOfMonth 1))]
    (loop [month last-month
           result []]
      (let [month-filter       (fn [x] (util/in-same-month-as month (:date x)))
            freq-filter       (fn [comp-fn x] (comp-fn (:freq x) :regular))
            income-this-month  (filter month-filter income)
            regular-income     (filter (partial freq-filter =) income-this-month)
            non-regular-income (filter (partial freq-filter not=) income-this-month)
            expend-this-month  (filter month-filter expend)
            tot-inc            (add-up income-this-month :in)
            reg-inc            (add-up regular-income :in)
            non-reg-inc        (add-up non-regular-income :in)
            tot-exp            (add-up expend-this-month :out)
            summary-this-month {:month month
                                :income tot-inc
                                :reg-inc reg-inc
                                :non-reg-inc non-reg-inc
                                :expend tot-exp}]
        (if (util/in-same-month-as first-month month)
          (conj result summary-this-month)
          (recur (.minusMonths month 1)
                 (conj result summary-this-month)))))))

(defn monthly-txn-summary->array [monthly-totals-map-list]
  (->> monthly-totals-map-list
       (sort-by :month)
       (map (fn [{:keys [month income expend]}]
              [(str month) income expend]))
       (#(conj % ["Month" "Income" "Expenditure"]))
       (into [])))

(defn monthly-statement [income expend month-end]
  (let [month-filter       (fn [x] (util/in-same-month-as month-end (:date x)))
        income-this-month  (filter month-filter income)
        expend-this-month  (filter month-filter expend)]
    (sort-by :seqno (concat income-this-month expend-this-month))))
