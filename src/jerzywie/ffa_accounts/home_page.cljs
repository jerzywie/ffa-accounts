(ns jerzywie.ffa-accounts.home-page
  (:require
   [jerzywie.ffa-accounts.file-upload :as fup]
   [jerzywie.ffa-accounts.date-picker :as d-p]
   [jerzywie.ffa-accounts.report-view :as report]
   [jerzywie.ffa-accounts.state :as state] ))

(defn home-page []
  [:div.app.container
   [:div.row
    [:header.d-flex.border-bottom.mb-3.mt-3
     [:a.me-3 {:href "https://bcrcp.org.uk/ffa/" :target "_blank"}
      [:img.img-fluid {:src "./images/FFA-logo.jpg" :alt "(FFA Logo)"}]]
     [:span.fs-2 "FFA Accounts"]]]
   (let [{:keys [file-name analysis-date data]} (state/state)]
     [:div
      [:div.row.border-bottom.d-print-none
       [:div.mb-3.col
        [fup/upload-btn file-name]]
       [:div.mb-3.col
        [d-p/month-picker-adaptive "Choose analysis date" :analysis-date]]]
      [:div.mb-3 (report/report data analysis-date)]
      [:hr]
      [state/debug-app-state]]
 )])
