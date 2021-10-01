(ns jerzywie.ffa-accounts.graph-view
  (:require
   [jerzywie.ffa-accounts.state :as state]
   [reagent.core :refer [atom]]))

(def data-changed (atom true))

(def more-data (atom 
                [["X-axis" "Y-axis"]
                 [1 20]
                 [2 12]
                 [3 48]
                 ]))


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
         (when (and this @data-changed)
           (prn "drawing data-changed?" @data-changed)
           (.draw (new (aget js/google.visualization chart-type) this)
                  (data-table data)
                  (clj->js options))
           (reset! data-changed false)))}]
     [:div "Loading..."])])

(defn do-graph []
  [:div.col {:id "chart-container"}
   [draw-chart "LineChart" @more-data {:title "My graph title"
                                      :hAxis {:title "X-axis"}
                                      :vAxis {:title "Y-axis"}}]])

