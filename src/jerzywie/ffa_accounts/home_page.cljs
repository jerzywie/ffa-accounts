(ns jerzywie.ffa-accounts.home-page
  (:require
   [cljs.pprint :refer [pprint]]
   [jerzywie.ffa-accounts.file-upload :as fup]
   [jerzywie.ffa-accounts.report :as report]
   [jerzywie.ffa-accounts.state :as state] ))

(defn home-page []
  [:div.app.container
   [:header.d-flex.border-bottom.mb-3
    [:img.img-fluid.me-3 {:src "./FFA-logo.jpg"}]
    [:span.fs-2.mt-3 "FFA Accounts"]]
   (let [{:keys [file-name data] :as state} @state/app-state]
     [:div
      [:div.topbar.hidden-print.mb-3
       [fup/upload-btn file-name]]
      [:div.mb-3 (report/report data)]
      [:div
       [:h5 "debug app state"]
       [:p (with-out-str (pprint state))]]]
     )])
