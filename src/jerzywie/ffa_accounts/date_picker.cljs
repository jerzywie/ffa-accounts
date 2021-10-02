(ns jerzywie.ffa-accounts.date-picker
  (:require
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.util :as util]))

(defn date-picker []
  (let [{:keys [analysis-date]} (state/state)]
    [:span.text-success
     [:label.me-1 {:for "date-picker"} "Choose analysis date"]
     [:input#date-picker.text-success
      {:type "date"
       :value (str analysis-date)
       :on-change (fn [e]
                    (state/add-analysis-date!
                     (util/parse-iso-date-string
                      (.-target.value e)))
                    (state/set-graph-data-changed!))}]]))
