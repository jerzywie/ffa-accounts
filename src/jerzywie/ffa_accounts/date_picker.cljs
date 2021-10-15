(ns jerzywie.ffa-accounts.date-picker
  (:require
   [jerzywie.ffa-accounts.state :as state]
   [jerzywie.ffa-accounts.util :as util]
   [reagent.core :as reagent]
   [goog.dom :as gdom]))

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


(defonce month-picker-state (reagent/atom {:native "" :fallback "d-none"}))
(defonce earliest-txn (util/md [2018 2 23]))
(defonce month-names ["January"
                      "February"
                      "March"
                      "April"
                      "May"
                      "June"
                      "July"
                      "August"
                      "September"
                      "October"
                      "November"
                      "December"])

(defn make-year-option-list [first-date last-date]
  (let [first-year (.year first-date)
        last-year  (.year last-date)]
    (for [y (range first-year (inc last-year))]
      ^{:key y}[:option (str y)])))

(defn make-option-list [names-list]
  (map (fn [n] ^{:key n}[:option n]) names-list))

(defn month-picker-adaptive []
  (reagent/create-class
   {:display-name "adaptive-month-picker"

    :component-did-mount
    (fn [_]
      (let [m-p (gdom/getElement "month-picker")]
        (if (= (.-type m-p) "month")
          (swap! month-picker-state assoc :native "" :fallback "d-none")
          (swap! month-picker-state assoc :native "d-none" :fallback ""))))

    :reagent-render
    (fn []
      (let [{:keys [analysis-date]} (state/state)]
        [:span.text-success
         [:div#native-month-picker {:class (:native @month-picker-state)}
          [:label.me-1 {:for "month-picker"} "Choose analysis month"]
          [:input#month-picker.text-success
           {:type "month"
            :value (do (apply str (take 7 (str analysis-date))))
            :on-change (fn [e]
                         (prn "m-p " (.-target.value e))
                         (state/add-analysis-date!
                          (-> (.-target.value e)
                              (str "-01")
                              util/parse-iso-date-string
                              util/last-date-in-month))
                         (state/set-graph-data-changed!))}]]
         [:div#fallback-month-picker {:class (:fallback @month-picker-state)}
          [:form {:on-submit (fn [e]
                               (.preventDefault e)
                               (state/add-analysis-date!
                                (->  [(.-value (:year @month-picker-state))
                                      (inc (.-selectedIndex (:month @month-picker-state)))
                                      1]
                                     util/md
                                     util/last-date-in-month)))}
           [:label.me-1 {:for "fallback-month"} "Choose analysis month"]
           [:select#fallback-month.text-success
            {:value (nth month-names (if (some? analysis-date)
                                       (dec (.monthValue analysis-date))
                                       0))
             :ref #(swap! month-picker-state assoc :month %)}
            (make-option-list month-names)]
           [:label.me-1.mb-4 {:for "fallback-year"} "year"]
           [:select#fallback-year.text-success
            {:value (if (some? analysis-date)
                      (str (.year analysis-date))
                      (str (.year earliest-txn)))
             :ref #(swap! month-picker-state assoc :year %)}
            (make-year-option-list earliest-txn (util/date-now))]
           [:input {:type "submit" :value "Go"}]]]]))}))
