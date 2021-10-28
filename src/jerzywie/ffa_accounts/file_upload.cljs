(ns jerzywie.ffa-accounts.file-upload
  (:require [jerzywie.ffa-accounts.csv :as mycsv] 
            [jerzywie.ffa-accounts.allocate :as alloc]
            [jerzywie.ffa-accounts.state :as state]
            [cljs.core.async :as async :refer [put! chan <!]]
            [goog.labs.format.csv :as csv])
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
  (state/remove-file-data!)
  (put! upload-reqs e))

(go-loop []
  (let [reader (js/FileReader.)
        file (<! upload-reqs)]
    (state/add-file-name! (.-name file))
    (set! (.-onload reader) #(put! file-reads %))
    (.readAsText reader file)
    (recur)))

(go-loop []
  (let [data (<! file-reads)]
    (state/add-data! data)
    (state/add-allocd-txns! (->> data
                                 :txns
                                 alloc/process-income))
    (state/add-exp! (->> data
                         :txns
                         alloc/process-expenditure))
    (state/set-chart-data-changed!)
    (recur)))

(defn upload-btn [file-name]
  [:span.upload-label
   [:label.btn.btn-outline-success.btn-sm {:type "button"}
    [:input.d-none
     {:type "file" :accept ".csv" :on-change put-upload}]
    [:i.fa.fa-upload.fa-lg.pe-2]
    (or file-name "Click here to load a transactions csv")]
   (when file-name
     [:i.fa.fa-times.ps-2 {:on-click #(state/remove-file-data!)}])])
