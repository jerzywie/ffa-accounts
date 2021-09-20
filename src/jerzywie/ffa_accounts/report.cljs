(ns jerzywie.ffa-accounts.report
  (:require
   [jerzywie.ffa-accounts.allocate :as alloc]
   [jerzywie.ffa-accounts.analyse :as anal]
   [jerzywie.ffa-accounts.util :as util]))

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
             (range))]])))
