(ns ^:figwheel-hooks jerzywie.ffa-accounts
  (:require
   [reagent.dom :as rdom]
   [jerzywie.ffa-accounts.home-page :as home]
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.util :as util]))

(defonce initialize
  (do
    (js/google.charts.load "current" (clj->js {:packages ["corechart"]}))
    (js/google.charts.setOnLoadCallback
     (fn google-visualization-loaded []
       (state/set-charts-ready!)))))

(defn multiply [a b] (* a b))

(defn get-app-element []
  (util/get-element-by-id "app"))

(defn mount [el]
  (rdom/render [home/home-page] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  (state/set-chart-data-changed!)
  (println "reload at " (str (util/time-now))))
