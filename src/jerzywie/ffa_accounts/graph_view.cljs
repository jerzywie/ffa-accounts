(ns jerzywie.ffa-accounts.graph-view
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
         (when (and this (state/is-graph-data-changed? chart-key))
           (prn "drawing data-changed?" (str chart-key) (state/is-graph-data-changed? chart-key))
           (.draw (new (aget js/google.visualization chart-type) this)
                  (data-table data)
                  (clj->js options))
          (state/reset-graph-data-changed! chart-key)
           ))}]
     [:div "Loading..."])])
