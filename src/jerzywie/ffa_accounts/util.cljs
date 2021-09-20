(ns jerzywie.ffa-accounts.util
  (:require [clojure.string :as s]
            [java.time :as j]
            [goog.string :as g]))

(def month-names ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn md
  "Helper function to make local-date from year month day array."
  [[y m d]]
  (.parse j/LocalDate (g/format "%04f-%02f-%02f" y m d)))

(defn strip-last-char-if [s char-string]
  (if (s/ends-with? s char-string)
    (apply str (take (dec (count s)) s))
    s))

(defn convert-txn-date-string
  "Process a date string in the format dd MMM yyyy"
  [dstr]
  (let [[dd MMM yyyy] (s/split dstr #"\s")
        MM (inc (.indexOf month-names MMM))]
    (.parse j/LocalDate (g/format "%s-%02i-%s" yyyy MM dd))))
