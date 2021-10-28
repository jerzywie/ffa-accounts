(ns jerzywie.ffa-accounts.chart-view
  (:require
   [jerzywie.ffa-accounts.state :as state]))

(defn data-table [data]
  (cond
    (map? data) (js/google.visualization.DataTable. (clj->js data))
    (string? data) (js/google.visualization.Query. data)
    (seqable? data) (js/google.visualization.arrayToDataTable (clj->js data))))

(defn draw-chart [chart-type chart-key data options ]
  [:div
   (if (:charts-ready? (state/state))
     [:div
      {:ref
       (fn [this]
         (when (and this (state/is-chart-data-changed? chart-key))
           (prn "Is chart data-changed?" (str chart-key) (state/is-chart-data-changed? chart-key))
           (.draw (new (aget js/google.visualization chart-type) this)
                  (data-table data)
                  (clj->js options))
          (state/reset-chart-data-changed! chart-key)
           ))}]
     [:div "Loading..."])])
