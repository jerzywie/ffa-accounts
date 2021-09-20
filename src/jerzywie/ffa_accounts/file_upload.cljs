(ns jerzywie.ffa-accounts.file-upload
  (:require [cljs.core.async :as async :refer [put! chan <!]]
            [goog.labs.format.csv :as csv]
            [jerzywie.ffa-accounts.csv :as mycsv]
            [jerzywie.ffa-accounts.state :as state])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def first-file
  (map (fn [e]
         (let [target (.-currentTarget e)
               file (-> target .-files (aget 0))]
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
  (put! upload-reqs e))

(go-loop []
  (let [reader (js/FileReader.)
        file (<! upload-reqs)]
    (swap! state/app-state assoc :file-name (.-name file))
    (set! (.-onload reader) #(put! file-reads %))
    (.readAsText reader file)
    (recur)))

(go-loop []
  (swap! state/app-state assoc :data (<! file-reads))
  (recur))

(defn upload-btn [file-name]
  [:span.upload-label
   [:label
    [:input.d-none
     {:type "file" :accept ".csv" :on-change put-upload}]
    [:i.fa.fa-upload.fa-lg.pe-2]
    (or file-name "Click here to upload the transactions csv...")]
   (when file-name
     [:i.fa.fa-times.ps-2 {:on-click #(state/reset-state!)}])])
