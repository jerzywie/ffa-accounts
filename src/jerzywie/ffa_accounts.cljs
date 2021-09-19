(ns ^:figwheel-hooks jerzywie.ffa-accounts
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [cljs.pprint :refer [pprint]]
   [cljs.core.async :as async :refer [alts! put! pipe chan <! >!]]
   [goog.labs.format.csv :as csv]
   [jerzywie.csv :as mycsv]
   [jerzywie.allocate :as alloc]
   [jerzywie.analyse :as anal]
   [jerzywie.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; define app state
(defonce initial-app-state {})
(defonce app-state (atom initial-app-state))

(def first-file
  (map (fn [e]
         (let [target (.-currentTarget e)
               file (-> target .-files (aget 0))]
           (prn "first-file target" target "file" file "name" (.-name file))
           (set! (.-value target) "")
           file))))

(def extract-result
  (map #(-> %
            .-target
            .-result
            csv/parse
            js->clj
            mycsv/transform-raw-data)))

(def upload-reqs (chan 1 first-file))
(def file-reads (chan 1 extract-result))

(defn put-upload [e]
  (prn "put-upload e" e)
  (put! upload-reqs e))

(go-loop []
  (let [reader (js/FileReader.)
        file (<! upload-reqs)]
    (swap! app-state assoc :file-name (.-name file))
    (set! (.-onload reader) #(put! file-reads %))
    (.readAsText reader file)
    (recur)))

(go-loop []
  (swap! app-state assoc :data (<! file-reads))
  (recur))

(defn upload-btn [file-name]
  [:span.upload-label
   [:label
    [:input.d-none
     {:type "file" :accept ".csv" :on-change put-upload}]
    [:i.fa.fa-upload.fa-lg.pe-2]
    (or file-name "Click here to upload the transactions csv...")]
   (when file-name
     [:i.fa.fa-times.ps-2 {:on-click #(reset! app-state initial-app-state)}])])

(defn multiply [a b] (* a b))

(defn report [data]
  (when data
    (let [analysis-date (util/md [2021 8 8])
          processed-transactions (->> data
                                      :txns
                                      alloc/process-income
                                      (anal/analyse-donations analysis-date))]
      [:div
       [:h4 (str "Donations as of " analysis-date)]
       [:h4 "Account summary"]
       [:div
        (map (fn [[k v] id] ^{:key id} [:p (str (name k) ": " v)]) (:accinfo data) (range))]])))

(defn get-app-element []
  (gdom/getElement "app"))

(defn hello-world []
  [:div.app
   [:h2 "FFA Accounts"]
   (let [{:keys [file-name data] :as state} @app-state]
     [:div
      [:div.topbar.hidden-print.mb-3
       [upload-btn file-name]]
      [:div.mb-3 (report data)]
      [:div
       [:h5 "debug app state"]
       [:p (with-out-str (pprint state))]]]
     )])

(defn mount [el]
  (rdom/render [hello-world] el))

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
