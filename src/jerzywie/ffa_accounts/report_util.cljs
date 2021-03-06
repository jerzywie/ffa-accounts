(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.analyse :as anal]
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
  "Calculate the aggregate weekly income given the value for a month."
  [month-value]
  (-> month-value (* 12) (/ 52)))

(defn get-summary-donation-totals
  "Return a list of maps, one for each different category."
  [txns]
  (let [summ-donations (->> txns
                            (group-by :period)
                            (map (fn [[k v]] {:name k
                                             :amount (add-up v :in)})))]
    summ-donations))

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
              [(util/date->MMM-yyyy month) income expend]))
       (#(conj % ["Month" "Income" "Expenditure"]))
       (into [])))

(defn monthly-statement [income expend month-end]
  (let [month-filter       (fn [x] (util/in-same-month-as month-end (:date x)))
        income-this-month  (filter month-filter income)
        expend-this-month  (filter month-filter expend)]
    (sort-by :seqno (concat income-this-month expend-this-month))))

(defn weekly-regular-donations [allocd-txns last-week-date num-weeks]
  (let [next-or-same-friday (util/get-next-day last-week-date util/js-friday)
        current? (fn [x] (:current x))
        reduce-fn (fn [result {:keys [name amount]}] (assoc result name amount))
        aggregate (fn [{:keys [weekly fortnightly monthly] :as rec}]
                    (assoc rec :aggregate (+ weekly
                                             (/ fortnightly 2)
                                             (calc-weekly-aggregate monthly))))]
    (loop [week next-or-same-friday
           count num-weeks
           result []]
      (if (= count 0)
        result
        (let [current-this-week (->> allocd-txns
                                 (anal/analyse-donations week)
                                 (filter current?))
              summ-totals (get-summary-donation-totals current-this-week)
              this-week (->> summ-totals
                             (reduce reduce-fn {})
                             (#(assoc % :date week))
                             aggregate)]
          (recur (.minusWeeks week 1)
                 (dec count)
                 (conj result this-week)))))))

(defn weekly-one-offs [income last-week-date num-weeks]
  (let [next-or-same-friday (util/get-next-day last-week-date util/js-friday)]
    (loop [week next-or-same-friday
           count num-weeks
           result []]
      (if (= count 0)
        result
        (let [filter-fn (fn [m] (and (util/within-last-period-of
                                     {:period :week}
                                     week
                                     (:date m))
                                    (not= (:freq m) :regular)))
              one-offs-this-week (filter filter-fn income)
              tot-1-offs (add-up one-offs-this-week :in)]
          (recur (.minusWeeks week 1)
                 (dec count)
                 (conj result {:one-offs tot-1-offs :date week})))))))

(defn weekly-expenditure [expend last-week-date num-weeks]
  (let [next-or-same-friday (util/get-next-day last-week-date util/js-friday)]
    (loop [week next-or-same-friday
           count num-weeks
           result []]
      (if (= count 0)
        result
        (let [filter-fn (fn [m] (util/within-last-period-of
                                 {:period :week}
                                 week
                                 (:date m)))
              expend-this-week (filter filter-fn expend)
              totals-expend (add-up expend-this-week :out)]
          (recur (.minusWeeks week 1)
                 (dec count)
                 (conj result {:expend totals-expend :date week})))))))

(defn safe-merge [m1 m2 m3]
  (let [date1 (:date m1)
        date2 (:date m2)
        date3 (:date m3)]
    (if (= date1 date2 date3)
      (merge m1 m2 m3)
      (throw (js/Error. "Incompatible dates in merge!")))))

(defn weekly-in-and-out->array [dated-donations-map-list
                                dated-one-offs-map-list
                                dated-expend-map-list]
  (let [merged-lists (map safe-merge
                          dated-donations-map-list
                          dated-one-offs-map-list
                          dated-expend-map-list)]
    (->> merged-lists
         (sort-by :date)
         (map (fn [{:keys [date aggregate one-offs expend]}]
                [(util/date->dd-MMM-yyyy date)
                 aggregate
                 one-offs
                 (+ aggregate one-offs) (* -1 expend)]))
         (#(conj % ["Week (Friday)" "Aggregate regular donations" "One-offs" "Total income" "Expenditure"]))
         (into []))))
