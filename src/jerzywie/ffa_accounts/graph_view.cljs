(ns jerzywie.ffa-accounts.graph-view
  (:require
   [jerzywie.ffa-accounts.state :as state]))

(defn data-table [data]
  (cond
    (map? data) (js/google.visualization.DataTable. (clj->js data))
    (string? data) (js/google.visualization.Query. data)
    (seqable? data) (js/google.visualization.arrayToDataTable (clj->js data))))

(defn draw-chart [chart-type data options]
  [:div
   (if (:charts-ready? (state/state))
     [:div
      {:ref
       (fn [this]
         (when (and this (:graph-data-changed (state/state)))
           (prn "drawing data-changed?" (:graph-data-changed (state/state)))
           (.draw (new (aget js/google.visualization chart-type) this)
                  (data-table data)
                  (clj->js options))
           (state/reset-graph-data-changed!)))}]
     [:div "Loading..."])])
