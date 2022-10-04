(ns jerzywie.ffa-accounts.parse-util
  (:require [jerzywie.ffa-accounts.util :as u]
            [clojure.string :as s]))

(defn convert-to-keyword [convert-map s]
  ((comp #(% convert-map)
         keyword
         s/lower-case
         #(u/strip-last-char-if % ":")
         (fn [item] (s/replace item #"\s" ""))) s))

(defn keywordise-transaction-headers [convert-map header-line]
  (map (partial convert-to-keyword convert-map) header-line))

(defn parse-amount [amount-string]
  (let [amount (re-find #"\d+\.?\d+" amount-string)]
    (if (s/blank? amount) nil (js/parseFloat amount))))
