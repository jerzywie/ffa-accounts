(ns jerzywie.ffa-accounts.csv-signature
  (:require [jerzywie.ffa-accounts.csv-nationwide :as nw]
            [jerzywie.ffa-accounts.csv-caf :as caf]
            [clojure.string :as s]))

(def file-signatures
  [{:bank               :caf
    :first-line         "Account Activity"
    :header-line-number 9
    :header-line        "Posting Date"}

   {:bank               :nationwide
    :first-line         "Account Name:"
    :header-line-number 4
    :header-line        "Date"}])

(defn check-sig [raw-data {:keys [bank first-line header-line-number header-line]}]
  (let [fl (-> raw-data first first)
        hl (-> (drop header-line-number raw-data) first first)
        result (and (s/includes? fl first-line) (s/includes? hl header-line))]
    {bank result}))

(defn identify-file-format [raw-data]
  (->> file-signatures
       (map (fn [sig] (check-sig raw-data sig)))
       (apply merge)
       (filter (fn [[_ v]] (true? v)))
       flatten
       first))

(defn transform-raw-data [raw-data]
  (condp = (identify-file-format raw-data)
    :caf (caf/transform-raw-data raw-data)
    :nationwide (nw/transform-raw-data raw-data)
    :else {:accinfo nil :bank :unknown :txns nil}))
