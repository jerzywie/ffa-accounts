(ns jerzywie.ffa-accounts.util
  (:require [clojure.string :as s]
            [java.time :as j]
            [java.time.temporal :as jt]
            [goog.string :as g]
            [goog.string.format]
            [goog.dom :as gdom]))

(def month-names ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn dbg-> [v msg]
  (prn "dbg-> " msg v)
  v)

(defn parse-iso-date-string
  "Parse date string of form 'yyyy-MM-dd'"
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

(defn convert-txn-date-string-caf
  "Process a date string in the format dd/mm/yyyy"
  [dstr]
  (let [[dd MM yyyy] (s/split dstr #"/")]
    (parse-iso-date-string (g/format "%s-%02i-%s" yyyy MM dd))))

(defn convert-txn-date-string
  "Process a date string in the format dd MMM yyyy."
  [dstr]
  (let [[dd MMM yyyy] (s/split dstr #"\s")
        MM (inc (.indexOf month-names MMM))]
    (parse-iso-date-string (g/format "%s-%02i-%s" yyyy MM dd))))

(defn date->MMM-yyyy
  "Convert a local-date to 'MMM-yyyy' string."
  [date]
  (str (->> (.monthValue date) dec (nth month-names)) "-" (.year date)))

(defn date->dd-MMM-yyyy
  "Convert a local-date to 'dd-MMM-yyyy' string."
  [date]
  (g/format "%02i-%s" (.dayOfMonth date) (date->MMM-yyyy date)))

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

(defn format-keyword [keyword]
  (-> keyword name s/capitalize))

(defn days-between [d1 d2]
  (.until d1 d2 (.. jt/ChronoUnit -DAYS)))

(defn is-within-dates? [first-date second-date test-date]
  (let [is-before? (.isBefore test-date first-date)
        is-after? (.isAfter test-date second-date)]
    (not (or is-before? is-after?))))

(defmulti within-last-period-of
  (fn [period reference-date test-date] (:period period)))

(defmethod within-last-period-of :month [_ reference-date test-date]
  (let [prev-month (.minusMonths reference-date 1)]
    (is-within-dates? prev-month reference-date test-date)))

(defmethod within-last-period-of :week [_ reference-date test-date]
  (let [prev-week (-> reference-date (.minusWeeks 1) (.plusDays 1))]
    (is-within-dates? prev-week reference-date test-date)))

(defn last-date-in-month [date]
  (-> date (.plusMonths 1) (.withDayOfMonth 1) (.minusDays 1)))


(defn in-same-month-as [reference-date test-date]
  (and (= (.year reference-date) (.year test-date))
       (= (.monthValue reference-date) (.monthValue test-date))))

(defn date-now []
  (. j/LocalDate now))

(defn time-now []
  (. j/LocalTime now))

(defn get-element-by-id [id]
  (gdom/getElement id))

(def js-monday    (.. j/DayOfWeek -MONDAY))
(def js-tuesday   (.. j/DayOfWeek -TUESDAY))
(def js-wednesday (.. j/DayOfWeek -WEDNESDAY))
(def js-thursday  (.. j/DayOfWeek -THURSDAY))
(def js-friday    (.. j/DayOfWeek -FRIDAY))
(def js-saturday  (.. j/DayOfWeek -SATURDAY))
(def js-sunday    (.. j/DayOfWeek -SUNDAY))

(defn get-next-day [date day]
  (.with date (.nextOrSame jt/TemporalAdjusters day)))
