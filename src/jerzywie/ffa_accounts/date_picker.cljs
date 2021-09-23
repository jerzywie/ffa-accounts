(ns jerzywie.ffa-accounts.date-picker
  (:require
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.util :as util]))

(defn date-picker []
  [:input
   {:type "date"
    :on-change (fn [e]
                 (state/add-analysis-date!
                  (util/parse-iso-date-string
                   (.-target.value e))))}])
