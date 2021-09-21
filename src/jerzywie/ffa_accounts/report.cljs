(ns jerzywie.ffa-accounts.report
  (:require
   [jerzywie.ffa-accounts.allocate :as alloc]
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]
   [jerzywie.ffa-accounts.cache :as cache]
   [clojure.string :refer [join trim replace starts-with? replace-first capitalize]]))

(defn format-account-name [name-set]
  (let [reducer (fn [s1 s2] (str s1 "|" s2))
        chop-start (fn [s start] (if (starts-with? s start) (replace-first s start "") s))]
    (-> name-set
        vec
        sort
        (#(reduce reducer nil %))
        (replace "||" "|")
        (chop-start "|")
        (replace "|" " & ")
        trim)))

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
              [:td (format-account-name account-name)]
              [:td.text-end (util/tonumber in)]
              [:td (format-freq freq)]
              [:td (str date)]]
             ))]))

(defn format-donor-amounts [amounts]
  (join ", "
        (reduce (fn [v [count amount]]
                  (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-total [amounts]
  (reduce (fn [v [count amount]] (+ v (* count amount))) 0 amounts))

(defn donor-report []
  (let [amount-analysis (fn [txns] (map (fn [[amount txn]] [(count txn) amount]) (group-by :in txns)))
        donor-fn (fn [[_ {:keys [names txns]}]] [names (count txns) (amount-analysis txns) (:date (last txns))])
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
              [:td (format-account-name account-name)]
              [:td.text-right count]
              [:td (format-donor-amounts amounts)]
              [:td.text-right (util/tonumber (get-total amounts) "£")]
              [:td (str date)]]))]))

(defn report [data]
  (when data
    (let [analysis-date (util/md [2021 8 8])
          processed-transactions (->> data
                                      :txns
                                      alloc/process-income
                                      (anal/analyse-donations analysis-date))]
      [:div
       [:h4 (str "Donations as of " analysis-date)]
       [:h4 "Account summary"]
       [:div
        (map (fn [[k v] id] ^{:key id} [:p (str (name k) ": " v)])
             (:accinfo data)
             (range))]
       [:h4 "Current donations"]
       (filter-donations processed-transactions
                         (fn [x] (contains? x :current)))
       [:h4 "One offs"]
       (filter-donations processed-transactions
                         (fn [x] (contains? (:freq x) :one-off)))
       [:h4 "Donor report"]
       (donor-report)
       ])))
