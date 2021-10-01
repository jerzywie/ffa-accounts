(ns ^:figwheel-hooks jerzywie.ffa-accounts
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [jerzywie.ffa-accounts.home-page :as home]
   [jerzywie.ffa-accounts.state :as state]))

(defonce initialize
  (do
    (js/google.charts.load (clj->js {:packages ["corechart"]}))
    (js/google.charts.setOnLoadCallback
     (fn google-visualization-loaded []
       (state/set-charts-ready!)))))

(defn multiply [a b] (* a b))

(defn get-app-element []
  (gdom/getElement "app"))

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
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
