(ns jerzywie.ffa-accounts.report-view
  (:require
   [jerzywie.ffa-accounts.allocate :as alloc]
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.report-util :as r-util] 
   [jerzywie.ffa-accounts.cache :as cache]
   [jerzywie.ffa-accounts.state :as state]
   [clojure.string :refer [capitalize]]))

(defn filter-donations [processed-txns filter-fn]
  (let [filtered-txns (filter filter-fn processed-txns)
        format-freq (fn [f-set] (-> f-set first name capitalize))]
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
              [:td (r-util/format-account-name account-name)]
              [:td.text-end (util/tonumber in)]
              [:td (format-freq freq)]
              [:td (str date)]]
             ))]))

(defn donor-report []
  (let [amount-analysis (fn [txns]
                          (map (fn [[amount txn]] [(count txn) amount])
                               (group-by :in txns)))
        donor-fn (fn [[_ {:keys [names txns]}]]
                   [names (count txns) (amount-analysis txns) (:date (last txns))])
        report-list (map donor-fn @cache/name-cache)]
    [:table.table.table-striped
     [:thead.table-light
      [:tr
       [:th {:scope "col"} "Account name"]
       [:th {:scope "col"} "Count"]
       [:th {:scope "col"} "Donations"]
       [:th {:scope "col"} "Total"]
       [:th {:scope "col"} "Last donation"]]]
     (into [:tbody]
           (for [[account-name count amounts date] report-list]
             [:tr
              [:td  account-name];(r-util/format-account-name)
              [:td.text-right count]
              [:td (r-util/format-donor-amounts amounts)]
              [:td.text-right (util/tonumber (r-util/get-total amounts) "£")]
              [:td (str date)]]))]))

(defn report [data analysis-date]
  (when (and data analysis-date)
    (let [allocd-txns (->> data
                           :txns
                           alloc/process-income)
          processed-transactions (anal/analyse-donations analysis-date allocd-txns)]
      (state/add-processed-transactions processed-transactions)
      [:div
       [:h4 (str "Donations as of " analysis-date)]
       [:h4 "Account summary"]
       [:div.row
        (map (fn [[k v] id] ^{:key id} [:div.col-md-4 (str (capitalize (name k)) ": " (util/tonumber v "£"))])
             (:accinfo data)
             (range))]
       [:div.row
        [:div.col-md-4]
        [:div.col-md-4 (str "First transaction: " (-> processed-transactions first :date str))]
        [:div.col-md-4 (str "Last transaction: " (-> processed-transactions last :date str))]]
       [:h4 "Current donations"]
       (filter-donations processed-transactions
                         (fn [x] (contains? x :current)))
       [:h4 "One offs"]
       (filter-donations processed-transactions
                         (fn [x] (contains? (:freq x) :one-off)))
       [:h4 "Donor report"]
       (donor-report)
       ])))