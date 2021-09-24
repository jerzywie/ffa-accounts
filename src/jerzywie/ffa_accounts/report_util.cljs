(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [clojure.string :refer [join trim replace starts-with? replace-first]] ))

(defn format-account-name [name-set]
  (let [reducer (fn [s1 s2] (str s1 "|" s2))
        chop-start (fn [s start]
                     (if (starts-with? s start)
                       (replace-first s start "")
                       s))]
    (-> name-set
        vec
        sort
        (#(reduce reducer nil %))
        (replace "||" "|")
        (chop-start "|")
        (replace "|" " & ")
        trim)))

(defn format-donor-amounts [amounts]
  (join ", "
        (reduce (fn [v [count amount]]
                  (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-total [amounts]
  (reduce (fn [v [count amount]] (+ v (* count amount))) 0 amounts))
