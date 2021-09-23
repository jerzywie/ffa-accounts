(ns jerzywie.ffa-accounts.state
  (:require [cljs.pprint :refer [pprint]]
            [reagent.core :as reagent :refer [atom]] ))

;;define app state
(defonce initial-app-state {})
(defonce app-state (atom initial-app-state))

(defn reset-state! []
  (reset! app-state initial-app-state))

(defn remove-file-data! []
  (swap! app-state dissoc :file-name :data :processed-txns))

(defn add-stuff! [k v]
  (swap! app-state assoc k v))

(defn add-analysis-date! [date]
  (add-stuff! :analysis-date date))

(defn add-file-name! [file-name]
  (add-stuff! :file-name file-name))

(defn add-data! [data]
  (add-stuff! :data data))

(defn add-processed-transactions [txns]
  (add-stuff! :processed-txns txns))

(defn debug-app-state []
  (let [state @app-state]
    [:div.mb-3
     [:h5 "debug app state"]
     [:div (with-out-str (pprint (assoc (dissoc state :data :processed-txns) :data-state  (if (:data state) "Data exists." "No data."))))]
     [:h6.mt-3 "Processed-txns ('type' and 'desc' omitted)"]
     [:table.table.table-sm.table-striped
      [:thead
       [:tr
        [:th {:scope "col"} "date"]
        ;[:th {:scope "col"} "type"]
        ;[:th {:scope "col"} "desc"]
        [:th.text-right {:scope "col"} "in"]
        [:th {:scope "col"} "name"]
        [:th {:scope "col"} "group"]
        [:th {:scope "col"} "account-name"]
        [:th {:scope "col"} "freq"]
        [:th {:scope "col"} "current"]]]
      (into [:tbody]
            (for [{:keys [date type desc in name group account-name freq current]} (:processed-txns state)]
              [:tr.tiny-words
               [:td (str date)]
               ;[:td type]
               ;[:td desc]
               [:td.text-right in]
               [:td name]
               [:td group]
               [:td account-name]
               [:td freq]
               [:td (when current "current")]]))]]))
