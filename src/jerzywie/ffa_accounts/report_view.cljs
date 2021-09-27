(ns jerzywie.ffa-accounts.report-view
  (:require
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.report-util :as r-util] 
   [jerzywie.ffa-accounts.cache :as cache]
   [jerzywie.ffa-accounts.state :as state]
   [clojure.string :refer [capitalize]]
   [clojure.pprint :refer [pprint]]))

(def caption-map {:weekly "Weekly regulars"
                  :monthly "Monthly regulars"
                  :weekly-grand-total "Net donations/week"
                  :monthly-grand-total "Net donations/month"
                  :new-amount "Grand total one-offs"
                  :one-off "Grand total one-offs"})

(defn filter-donations [processed-txns filter-fn]
  (let [filtered-txns (filter filter-fn processed-txns)
        format-freq (fn [f-set] (-> f-set first name capitalize))]
    [:div
     [:table.table.table-striped
      [:thead.table-light
       [:tr
        [:th "Account name"]
        [:th.text-end "Amount"]
        [:th "Frequency"]
        [:th "Last paid"]]]
      (into [:tbody]
            (for [{:keys [freq date account-name in]} filtered-txns]
              [:tr
               [:td account-name]
               [:td.text-end (util/tonumber in)]
               [:td (format-freq freq)]
               [:td (str date)]]
              ))]
     [:h5 "Summary"]
     [:div.row.mb-3
      (map (fn [[k v] id]
             (when (> v 0)
               ^{:key id} [:div.col
                (str (k caption-map) ": " (util/tonumber v "£"))]))
           (r-util/get-summary-donation-totals filtered-txns)
           (range))]]))

(defn donor-report []
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
              [:td.text-right (util/tonumber (r-util/get-donor-amount-total amounts) "£")]
              [:td (str f-date)]
              [:td (str l-date)]]))]))

(defn expenditure-report [txns filter-fn]
  (let [filtered-txns (filter filter-fn txns)]
    [:div
     [:table.table.table-striped
      [:thead.table-light
       [:tr
        [:th "Date"]
        [:th "Payment to"]
        [:th "Payment type"]
        [:th "Amount"]]]
      (into [:tbody]
            (for [{:keys [date desc type out]} filtered-txns]
              [:tr
               [:td (str date)]
               [:td desc]
               [:td type]
               [:td.text-right (util/tonumber out)]]))]
     [:h5 "Summary"]
     [:div.row.mb-3
      (map (fn [[k v] id] ^{:key id}
             [:div.col (str k ": " (util/tonumber v "£"))])
           (r-util/get-summary-expenditure-totals filtered-txns)
           (range))]
     ]))

(defn report [data analysis-date-or-nil]
  (when data
    (let [allocd-txns (:allocd-txns (state/state))
          date-first-txn (-> data :txns first :date)
          date-last-txn (-> data :txns last :date)
          analysis-date (or analysis-date-or-nil date-last-txn)]
      (let [processed-transactions (anal/analyse-donations analysis-date allocd-txns)]
           (state/add-processed-transactions processed-transactions)
           (state/add-analysis-date! analysis-date)
           [:div
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
            [:h4 "Regular donations in last month"]
            (filter-donations processed-transactions
                              (fn [x] (contains? x :current)))
            [:h4 "One off amounts in last month"]
            (filter-donations processed-transactions
                              (fn [x] (and (contains? (:freq x) :one-off)
                                          (util/within-last-month-of analysis-date (:date x)))))
            [:h4 "Expenditure in last month"]
            [expenditure-report
             (:exp (state/state))
             (fn [x] (util/within-last-month-of analysis-date (:date x)))]
            [:h4 "Donor report"]
            (donor-report)
            ]))))
