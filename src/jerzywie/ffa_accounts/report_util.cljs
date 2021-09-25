(ns jerzywie.ffa-accounts.report-util
  (:require
   [jerzywie.ffa-accounts.util :as util]
   [clojure.string :refer [join trim replace starts-with? replace-first]] ))

(defn format-account-name [name-set]
  (let [chop-start (fn [s start] (if (starts-with? s start)
                                  (replace-first s start "")
                                  s))]
    (-> name-set
        vec
        sort
        (#(join "|" %))
        trim
        (replace "||" "|")
        (chop-start "|")
        (replace "|" " & "))))

(defn format-donor-amounts [amounts]
  (join ", "
        (reduce (fn [v [count amount]]
                  (conj v (str count " x " (util/tonumber amount "£")))) [] amounts)))

(defn get-total [amounts]
  (reduce (fn [v [count amount]] (+ v (* count amount))) 0 amounts))
