(ns jerzywie.ffa-accounts.state
  (:require [cljs.pprint :refer [pprint]]
            [reagent.core :as reagent :refer [atom]] ))

;;define app state
(defonce initial-app-state {})
(defonce chart-keys [:monthly-chart :exp-chart])
(defonce app-state (atom initial-app-state))

(defn state []
  @app-state)

(defn reset-state! []
  (reset! app-state initial-app-state))

(defn remove-file-data! []
  (swap! app-state dissoc :file-name :data :processed-txns :analysis-date))

(defn add-stuff! [k v]
  (swap! app-state assoc k v))

(defn add-analysis-date! [date]
  (add-stuff! :analysis-date date))

(defn add-file-name! [file-name]
  (add-stuff! :file-name file-name))

(defn add-data! [data]
  (add-stuff! :data data))

(defn add-processed-transactions! [txns]
  (add-stuff! :processed-txns txns))

(defn add-allocd-txns! [txns]
  (add-stuff! :allocd-txns txns))

(defn add-exp! [txns]
  (add-stuff! :exp txns))

(defn set-charts-ready! []
  (add-stuff! :charts-ready? true))

(defn set-graph-data-changed! []
  (add-stuff! :graph-data-changed (zipmap chart-keys (repeat true))))

(defn reset-graph-data-changed! [chart-key]
  (swap! app-state assoc-in [:graph-data-changed chart-key] false))

(defn is-graph-data-changed? [chart-key]
  (get-in @app-state [:graph-data-changed chart-key]))

(defn debug-app-state []
  (when ^boolean js/goog.DEBUG
    (let [state @app-state]
      [:pre.tiny-words
       [:div.mb-3.d-print-none
        [:h5 "debug app state"]
        [:div (with-out-str (pprint (assoc
                                     (dissoc state :data :processed-txns :allocd-txns :exp)
                                     :data-state  (if (:data state) "Data exists." "No data."))))]
        [:h6.mt-3 "Processed-txns ('type' and 'desc' omitted)"]
        [:table.table.table-sm.table-striped
         [:thead
          [:tr
           [:th {:scope "col"} "date"]
           [:th.text-right {:scope "col"} "in"]
           [:th {:scope "col"} "name"]
           [:th {:scope "col"} "group"]
           [:th {:scope "col"} "account-name"]
           [:th {:scope "col"} "period"]
           [:th {:scope "col"} "freq"]
           [:th {:scope "col"} "new?"]
           [:th {:scope "col"} "current?"]
           [:th {:scope "col"} "seqno"]]]
         (into [:tbody]
               (for [{:keys [date in name group account-name period freq new current seqno]}
                     (:processed-txns state)]
                 [:tr.tiny-words
                  [:td (str date)]
                  [:td.text-right in]
                  [:td name]
                  [:td group]
                  [:td account-name]
                  [:td period]
                  [:td freq]
                  [:td (when new "new!")]
                  [:td (when current "current")]
                  [:td seqno]]))]
        [:h6.mt3 ":allocd-txns"]
        [:div (with-out-str (pprint (:allocd-txns state)))]
        [:h6.mt3 ":exp"]
        [:div (with-out-str (pprint (:exp state)))]]])))
