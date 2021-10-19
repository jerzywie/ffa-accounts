(ns jerzywie.ffa-accounts.report-view
  (:require
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.report-util :as r-util] 
   [jerzywie.ffa-accounts.cache :as cache]
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.graph-view :as graph-view]
   [clojure.string :refer [capitalize]]
   [clojure.pprint :refer [pprint]]))

(def caption-map {:weekly "Weekly SOs"
                  :monthly "Monthly SOs"
                  :fortnightly "Fortnightly SOs"
                  :weekly-grand-total "Net donations/week"
                  :monthly-grand-total "Net donations/month"
                  :weekly-aggregate "Aggregate weekly income"
                  :new-amount "Grand total one-offs"
                  :one-off "Grand total one-offs"})

(defn report-donations [processed-txns filter-fn]
  (let [filtered-txns (filter filter-fn processed-txns)
        summ-donations (r-util/get-summary-donation-totals filtered-txns)]
    [:div
     [:h5 "Summary"]
     [:div.col (with-out-str (pprint summ-donations))]
     [:div.row.mb-3
      ;(map (fn [{:keys [name amount]} id]
       ;      (when (> amount 0)
        ;       ^{:key id}
         ;      [:div.col
          ;      (str (name caption-map) ": " (util/tonumber amount "£"))]))
           ;summ-donations
           ;(range))
      ]

     [:h5 "Detail"]
     [:table.table.table-striped
      [:thead.table-light
       [:tr
        [:th "Account name"]
        [:th.text-end "Amount"]
        [:th "Frequency"]
        [:th "Last paid"]]]
      (into [:tbody]
            (let [format-period (fn [period] period ;(-> period name capitalize)
                                  )]
              (for [{:keys [period date account-name in]} filtered-txns]
                [:tr
                 [:td account-name]
                 [:td.text-end (util/tonumber in)]
                 [:td (format-period period)]
                 [:td (str date)]])))]]))

(defn report-donors []
  (let [amount-analysis (fn [txns]
                          (map (fn [[amount txn]] [(count txn) amount])
                               (group-by :in txns)))
        donor-fn (fn [[_ {:keys [txns]}]]
                   [(:account-name (first txns))
                    (count txns)
                    (amount-analysis txns)
                    (:date (first txns))
                    (:date (last txns))])
        report-list (sort (map donor-fn @cache/name-cache))]
    [:table.table.table-striped
     [:thead.table-light
      [:tr
       [:th {:scope "col"} "Account name"]
       [:th {:scope "col"} "Count"]
       [:th {:scope "col"} "Donations"]
       [:th {:scope "col"} "Total"]
       [:th {:scope "col"} "First donation"]
       [:th {:scope "col"} "Last donation"]]]
     (into [:tbody]
           (for [[account-name count amounts f-date l-date] report-list]
             [:tr
              [:td account-name]
              [:td.text-right count]
              [:td (r-util/format-donor-amounts amounts)]
              [:td.text-end (util/tonumber (r-util/get-donor-amount-total amounts) "£")]
              [:td (str f-date)]
              [:td (str l-date)]]))]))

(defn report-expenditure [txns filter-fn]
  (let [filtered-txns (filter filter-fn txns)
        summ-exp (r-util/get-summary-expenditure-totals filtered-txns)]
    [:div
     [:h5 "Summary"]
     [:div.row.mb-3
      (map (fn [{:keys [name amount]} id] ^{:key id}
             [:div.col (str name ": " (util/tonumber amount "£"))])
           summ-exp
           (range))]
                                        ;[:div.col (with-out-str (pprint summ-exp))]
     [:div.row
      [:div.col
       (let [plot-data (r-util/summary-totals->array summ-exp)]
         [graph-view/draw-chart "PieChart" plot-data {:title "Expenditure"}])]]
     [:h5 "Detail"]
     [:table.table.table-striped
      [:thead.table-light
       [:tr
        [:th "Date"]
        [:th "Payment to"]
        [:th "Payment type"]
        [:th.text-end "Amount"]]]
      (into [:tbody]
            (for [{:keys [date desc type out]} filtered-txns]
              [:tr
               [:td (str date)]
               [:td desc]
               [:td type]
               [:td.text-end (util/tonumber out)]]))]]))

(defn txn-summary [income expenditure]
  (let [tot-in (r-util/add-up income :in)
        tot-out (r-util/add-up expenditure :out)]
    [:div.row
     [:div.col (str "Total in for month: " (util/tonumber tot-in "£"))]
     [:div.col (str "Total out for month: " (util/tonumber tot-out "£"))]
     [:div.col (str "Weekly aggregate income: " (-> income
                                                    r-util/calc-weekly-aggregate
                                                    (util/tonumber "£")))]]))

(defn monthly-txn-summary-view [income expend date-first-txn date-last-txn]
  (let [txn-summary (r-util/monthly-txn-summary income expend date-first-txn date-last-txn)]
    [:table.table.table-striped
     [:thead
      [:tr
       [:th "Month"]
       [:th.text-end "Donations"]
       [:th.text-end "Regular"]
       [:th.text-end "Occasional/One-Off"]
       [:th.text-end "Expenditure"]
       [:th.text-end "Inc - Exp"]]]
     (into [:tbody]
           (for [{:keys [month income reg-inc non-reg-inc expend]} txn-summary]
             [:tr
              [:td (apply str (take 7 (str month)))]
              [:td.text-end (util/tonumber income)]
              [:td.text-end (util/tonumber reg-inc)]
              [:td.text-end (util/tonumber non-reg-inc)]
              [:td.text-end (util/tonumber expend)]
              [:td.text-end (util/tonumber (- income expend))]]))]))

(defn new-report [data analysis-date-or-nil]
  (when data
    (let [allocd-txns (:allocd-txns (state/state))
          expend (:exp (state/state))
          date-first-txn (-> data :txns first :date)
          date-last-txn (-> data :txns last :date)
          analysis-date (or analysis-date-or-nil date-last-txn)
          processed-transactions (anal/analyse-donations analysis-date allocd-txns)]
      [:div
       [:div.row
        [:div.col
         [:h4 "Account summary"]
         [:div.row
          (map (fn [[k v] id] ^{:key id}
                 [:div.col-md-4 (str (capitalize (name k)) ": " (util/tonumber v "£"))])
               (:accinfo data)
               (range))]
         [:div.row
          [:div.col-md-4]
          [:div.col-md-4 (str "First transaction: " date-first-txn)]
          [:div.col-md-4 (str "Last transaction: " date-last-txn)]]

         [:h4 "Monthly summary"]
         [:div [monthly-txn-summary-view processed-transactions
                expend
                date-first-txn
                date-last-txn]]]]])))

(defn report [data analysis-date-or-nil]
  (when data
    (let [allocd-txns (:allocd-txns (state/state))
          date-first-txn (-> data :txns first :date)
          date-last-txn (-> data :txns last :date)
          analysis-date (or analysis-date-or-nil date-last-txn)
          processed-transactions (anal/analyse-donations analysis-date allocd-txns)
          last-months-txns (fn [x] (util/in-same-month-as analysis-date (:date x)))
          income-in-month (filter last-months-txns processed-transactions)
          expenditure-in-month (filter last-months-txns (:exp (state/state)))]
      (state/add-processed-transactions! processed-transactions)
      (state/add-analysis-date! analysis-date)
      [:div
       [:div.row
        [:div.col
         [:h4 (str "Donations as of " analysis-date)]

         [:h4 "Account summary"]
         [:div.row
          (map (fn [[k v] id] ^{:key id} [:div.col-md-4 (str (capitalize (name k)) ": " (util/tonumber v "£"))])
               (:accinfo data)
               (range))]
         [:div.row
          [:div.col-md-4]
          [:div.col-md-4 (str "First transaction: " date-first-txn)]
          [:div.col-md-4 (str "Last transaction: " date-last-txn)]]

         [:h4 "Monthly summary"]
         [txn-summary income-in-month expenditure-in-month]

         [:h4 "Current regular donations"]
         [report-donations
          processed-transactions
          (fn [x] (contains? x :current))]

         [:h4 "One off amounts in last month"]
         [report-donations
          processed-transactions
          (fn [x] (and (not= (:freq x) :regular)
                      (util/in-same-month-as analysis-date (:date x))))]

         [:h4 "Expenditure in last month"]
         [report-expenditure
          (:exp (state/state))
          (fn [x] (util/in-same-month-as analysis-date (:date x)))]

         [:h4 "Donor report"]
         [report-donors]]]])))
