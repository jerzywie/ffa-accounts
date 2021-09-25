(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [clojure.string :as s]))

(defn format-donor-amounts [amounts]
  (s/join ", "
        (reduce (fn [v [count amount]]
                  (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-total [amounts]
  (reduce (fn [v [count amount]] (+ v (* count amount))) 0 amounts))
