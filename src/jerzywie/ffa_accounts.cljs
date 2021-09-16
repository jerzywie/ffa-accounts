(ns ^:figwheel-hooks jerzywie.ffa-accounts
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [cljs.pprint :refer [pprint]]
   [cljs.core.async :as async :refer [alts! put! pipe chan <! >!]]
   [goog.labs.format.csv :as csv])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(println "This text is printed from src/jerzywie/ffa_accounts.cljs. Go ahead and edit it and see reloading in action!")

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!" :file-name "file-name.csv" :data "blah"}))

(def first-file
  (map (fn [e]
         (let [target (.-currentTarget e)
               file (-> target .-files (aget 0))]
           (set! (.-value target) "")
           file))))

(def extract-result
  (map #(-> % .-target .-result csv/parse js->clj)))

(def upload-reqs (chan 1 first-file))
(def file-reads (chan 1 extract-result))

(defn put-upload [e]
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
    [:input.hidden-xs-up 
     {:type "file" :accept ".csv" :on-change put-upload}]
    [:i.fa.fa-upload.fa-lg]
    (or file-name "click here to upload and render csv...")]
   (when file-name 
     [:i.fa.fa-times {:on-click #(reset! app-state {})}])])

(defn multiply [a b] (* a b))

(defn report [data]
  (with-out-str (pprint data)))

(defn get-app-element []
  (gdom/getElement "app"))

(defn hello-world []
  [:div.app
   [:h1 (:text @app-state)]
   [:h3 "Edit this in src/jerzywie/ffa_accounts.cljs and watch! Has it changed!?!"]
   (let [{:keys [file-name data] :as state} @app-state]
     [:div
      [:h4 "debug app state"]
      [:p (with-out-str (pprint state))]
      [:div.topbar.hidden-print 
       [upload-btn file-name]]
      [:p (report data)]])])

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
