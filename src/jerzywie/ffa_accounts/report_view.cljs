(ns jerzywie.ffa-accounts.report-view
  (:require
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.report-util :as r-util] 
   [jerzywie.ffa-accounts.cache :as cache]
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.chart-view :as chart-view]
   [jerzywie.ffa-accounts.date-picker :as d-p]
   [clojure.string :refer [capitalize]]
   [clojure.pprint :refer [pprint]]))

(def caption-map {:weekly "Weekly SOs"
                  :monthly "Monthly SOs"
                  :fortnightly "Fortnightly SOs"
                  :weekly-grand-total "Net donations/week"
                  :monthly-grand-total "Net donations/month"
                  :weekly-aggregate "Aggregate weekly income"
                  :none "Grand total one-offs"
                  :one-off "Grand total one-offs"})

(defn account-summary [accinfo income expend date-first-txn date-last-txn]
  [:div
   [:div.row
    (map (fn [[k v] id] ^{:key id}
           [:div.col-md-4 (str (capitalize (name k)) ": " (util/tonumber v "£"))])
         accinfo
         (range))]
   [:div.row
    [:div.col-md-4]
    [:div.col-md-4 (str "First transaction: " (util/date->dd-MMM-yyyy date-first-txn))]
    [:div.col-md-4 (str "Last transaction: " (util/date->dd-MMM-yyyy date-last-txn))]]
   [:div.row
    [:div.col-md-4]
    [:div.col-md-4 (str "Income: " (util/tonumber (r-util/add-up income :in) "£"))]
    [:div.col-md-4 (str "Expenditure: " (util/tonumber (r-util/add-up expend :out) "£"))]]
])

(defn report-donations [income filter-fn sort-by-key]
  (let [filtered-txns (sort-by sort-by-key (filter filter-fn income))
        summ-donations (r-util/get-summary-donation-totals filtered-txns)]
    [:div
     [:h5 "Summary"]
     [:div.row.mb-3
      ^{:key 999}[:div.col (str "Number of donors: " (count filtered-txns))]
      (map (fn [{:keys [name amount]} id]
             (when (> amount 0)
               ^{:key id}
               [:div.col
                (str (name caption-map) ": " (util/tonumber amount "£"))]))
           summ-donations
           (range))]

     [:h5 "Detail"]
     [:table.table.table-striped
      [:thead.table-light
       [:tr
        [:th "Account name"]
        [:th.text-end "Amount"]
        [:th "Frequency"]
        [:th "Last paid"]]]
      (into [:tbody]
            (for [{:keys [period date account-name in]} filtered-txns]
              [:tr
               [:td account-name]
               [:td.text-end (util/tonumber in)]
               [:td (util/format-keyword period)]
               [:td (util/date->dd-MMM-yyyy date)]]))]]))

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
              [:td (util/date->dd-MMM-yyyy f-date)]
              [:td (util/date->dd-MMM-yyyy l-date)]]))]))

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
     [:div.row
      [:div.col
       (let [plot-data (r-util/summary-totals->array summ-exp)]
         [chart-view/draw-chart "PieChart" :exp-chart plot-data {:title "Expenditure"}])]]
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
               [:td (util/date->dd-MMM-yyyy date)]
               [:td desc]
               [:td type]
               [:td.text-end (util/tonumber out)]]))]]))

(defn monthly-txn-summary-view [income expend date-first-txn date-last-txn]
  (let [txn-summary (r-util/monthly-txn-summary income expend date-first-txn date-last-txn)]
    [:div
     [:table.table
      [:thead.black-border-bottom.table-light
       [:tr
        [:th.text-end    {:rowSpan 2} "Month"]
        [:th.text-center {:colSpan 3} "Income (Donations)"]
        [:th.text-end    {:rowSpan 2} "Expenditure"]
        [:th.text-end    {:rowSpan 2} "Income over Expenditure"]
        [:th.text-center {:colSpan 2} "Weekly averages"]]
       [:tr
        [:th.text-end "Regular"]
        [:th.text-end "Occasional/One-Off"]
        [:th.text-end "Total"]
        [:th.text-end "Inc"]
        [:th.text-end "Exp"]]]
      (into [:tbody]
            (for [{:keys [month income reg-inc non-reg-inc expend]} txn-summary]
              (let [inc-exp-diff (- income expend)]
                [:tr.text-end
                 {:class (if (pos? inc-exp-diff) "table-success" "table-danger")}
                 [:td (util/date->MMM-yyyy month)]
                 [:td (util/tonumber reg-inc)]
                 [:td (util/tonumber non-reg-inc)]
                 [:td (util/tonumber income)]
                 [:td (util/tonumber expend)]
                 [:td (util/tonumber inc-exp-diff)]
                 [:td (-> income r-util/calc-weekly-aggregate util/tonumber)]
                 [:td (-> expend r-util/calc-weekly-aggregate util/tonumber)]
                 ])))]
     (let [plot-data (r-util/monthly-txn-summary->array txn-summary)]
       [chart-view/draw-chart "AreaChart"
        :monthly-chart
        plot-data
        {:title "Month by Month Income and Expenditure"
         :colors ["green" "red"]
         :hAxis {:title "Month"}
         :vAxis {:title "Pounds"}
         :backgroundColor {:strokeWidth 2}}]
       )]))

(defn monthly-statement-view [income expend month-end]
  (let [months-txns (r-util/monthly-statement income expend month-end)]
    [:div
     [:table.table.table-striped.table-sm
      [:thead.table-light
       [:tr
        [:th "Date"]
        [:th "Type"]
        [:th "Name"]
        [:th.text-end "Money in"]
        [:th.text-end "Money Out"]
        [:th.text-end "Balance"]]]
      (into [:tbody]
            (map (fn [{:keys [date type period freq desc account-name in out bal]}]
                   (let [fmt-income-type (fn []
                                           (when period
                                             (-> (if (= period :none)
                                                   freq
                                                   period)
                                                 util/format-keyword)))
                         vary-in-out (fn [in-text out-text] (if (nil? in)
                                                             out-text
                                                             in-text))]
                     [:tr
                      [:td (util/date->dd-MMM-yyyy date)]
                      [:td (vary-in-out (fmt-income-type) type)]
                      [:td (vary-in-out account-name desc)]
                      [:td.text-end (util/tonumber in)]
                      [:td.text-end (util/tonumber out)]
                      [:td.text-end (util/tonumber bal)]])) months-txns))
      [:tfoot.table-light
       [:tr
        [:td.fw-bold {:colSpan 3} "T O T A L S"]
        [:td.text-end (-> months-txns (r-util/add-up :in) (util/tonumber))]
        [:td.text-end (-> months-txns (r-util/add-up :out) (util/tonumber))]
        [:td ""]]]]]))

(defn weekly-analysis [donations expenditure analysis-date num-weeks]
  (let [weekly-in (r-util/weekly-regular-donations donations analysis-date num-weeks)
        plot-data (r-util/weekly-regular-donations->array weekly-in)]
    (-> weekly-in
        pprint
        with-out-str)
    [chart-view/draw-chart "AreaChart"
     :weekly-analysis
     plot-data
     {:title "weekly"}]
    ))

(defn report [data analysis-date-or-nil]
  (when data
    (let [allocd-txns (:allocd-txns (state/state))
          expend (:exp (state/state))
          date-first-txn (-> data :txns first :date)
          date-last-txn (-> data :txns last :date)
          analysis-date (or analysis-date-or-nil date-last-txn)
          income (anal/analyse-donations analysis-date allocd-txns)]
      (state/add-processed-transactions! income)
      (state/add-analysis-date! analysis-date)
      (when-not analysis-date-or-nil (state/add-stuff! :statement-month analysis-date))
      [:div
       [:div.row
        [:div.col
         [:h4.mt-4 "Account summary"]
         [account-summary (:accinfo data) income expend date-first-txn date-last-txn]

         [:h4.mt-4 "Monthly summary"]
         [monthly-txn-summary-view income expend date-first-txn date-last-txn]

         [:div.row.row-cols-2.mt-4
          [:div.col
           [:h4(str "Monthly transactions for " (util/date->MMM-yyyy (:statement-month (state/state))))]]
          [:div.col.d-print-none
           [d-p/month-picker-adaptive nil :statement-month]]]

         [monthly-statement-view income
          expend
          (:statement-month (state/state))]

         [:h4.mt-4 "Current regular donations"]
         [report-donations
          income
          (fn [x] (contains? x :current))
          :period]

         [:h4.mt-4 "One off amounts in last month"]
         [report-donations
          income
          (fn [x] (and (not= (:freq x) :regular)
                      (util/in-same-month-as analysis-date (:date x))))
          :date]

         [:h4.mt-4 "Expenditure in last month"]
         [report-expenditure
          (:exp (state/state))
          (fn [x] (util/in-same-month-as analysis-date (:date x)))]

         [:h4.mt-4 "Donor report"]
         [report-donors]

         [:h4.mt-4 "Weekly donation analysis"]
         [weekly-analysis allocd-txns expend analysis-date 12]
         ]]])))
