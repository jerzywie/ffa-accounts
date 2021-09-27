(ns jerzywie.ffa-accounts.util
  (:require [clojure.string :as s]
            [java.time :as j]
            [java.time.temporal :as jt]
            [goog.string :as g]
            [goog.string.format]))

(def month-names ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn dbg-> [v msg]
  (prn "dbg-> " msg v)
  v)

(defn parse-iso-date-string
  "Parse date string of form 'yyyy-mm-dd'"
  [iso-date-string]
  (.parse j/LocalDate iso-date-string))

(defn md
  "Helper function to make local-date from year month day array."
  [[y m d]]
  (parse-iso-date-string (g/format "%04f-%02f-%02f" y m d)))

(defn strip-last-char-if [s char-string]
  (if (s/ends-with? s char-string)
    (apply str (take (dec (count s)) s))
    s))

(defn convert-txn-date-string
  "Process a date string in the format dd MMM yyyy"
  [dstr]
  (let [[dd MMM yyyy] (s/split dstr #"\s")
        MM (inc (.indexOf month-names MMM))]
    (parse-iso-date-string (g/format "%s-%02i-%s" yyyy MM dd))))

(defn tonumber
  ([v curr]
   (cond
     (nil? v) ""
     (js/isNaN v) v
     v (g/format "%s%0.2f" curr v)
     :else ""))
  ([v]
   (tonumber v "")))

(defn format-account-name [name-set]
  (let [chop-start (fn [s start] (if (s/starts-with? s start)
                                   (s/replace-first s start "")
                                   s))]
    (-> name-set
        vec
        sort
        (#(s/join "|" %))
        s/trim
        (s/replace "||" "|")
        (chop-start "|")
        (s/replace "|" " & "))))

(defn days-between [d1 d2]
  (.until d1 d2 (.. jt/ChronoUnit -DAYS)))

(defn within-last-month-of [analysis-date test-date]
  (let [prev-month (.minusMonths analysis-date 1)
        is-before? (.isBefore test-date prev-month)
        is-after? (.isAfter test-date analysis-date)]
    (not (or is-before? is-after?))))
