(ns jerzywie.ffa-accounts.allocate
  (:require [jerzywie.ffa-accounts.cache :as nc]
            [jerzywie.ffa-accounts.util :as util]
            [clojure.string :as s]))

(def expend-map {"Gloucester Rd Fruiterers"
                 #{"GLOUCESTER ROAD FR" "FAHID REHMAN"}
                 "Good Egg Company"
                 #{"THE GOOD EGG COMPA" "GOOD EGG COMPANY"}})

(def empty-name {:names #{} :group nil :filterby nil})

(def bank-credit "Bank credit")
(def transfer-from "Transfer from")

(defn strip-prefix [text]
  (-> text
      (s/replace bank-credit "")
      (s/replace transfer-from "")
      (util/strip-last-char-if "&")
      s/trim))

(defn make-group
  "If the name and description are the same, then group is nil
   If they are different, then try for a group id by
   extracting account details."
  [name desc-less-prefix]
  (let [maybe-group (if (not= name desc-less-prefix) desc-less-prefix nil)]
    (if (some? maybe-group)
      (re-find #"\d{6} \d{8}" maybe-group)
      nil)))

(defn process-name [{:keys [type desc]}]
  (let [name (strip-prefix type)
        strip-desc (strip-prefix desc)
        group (make-group name strip-desc)]
    {:name name :group group}))

(defn make-key [{:keys [name group]}]
  (hash (if (nil? group) name group)))

(defn cache-name [{:keys [name group] :as m}]
  (let [key (make-key m)
        value (nc/get-cache-value key empty-name)
        new-value (assoc value :names (conj (:names value) name)
                         :group group
                         :filterby (if (nil? group) :names :group))]
    (nc/cache! key new-value)))

(defn make-filter-fn [{:keys [names group filterby]}]
  (case filterby
    :names (fn [txn] (= (:name txn) (first names)))
    :group (fn [txn] (= (:group txn) group))))

(defn add-transactions [txns key]
  (let [entity (nc/get-cache-value key)
        account-name (util/format-account-name (:names entity))
        filter-fn (make-filter-fn entity)
        filtered-txns (->> (filter filter-fn txns)
                           (map #(assoc % :account-name account-name)))]
    (nc/cache! key (assoc entity :txns filtered-txns))))

(defn process-income [transactions]
  (nc/empty-cache)
  (let [raw-in-txns (filter #(nil? (:out %)) transactions)
        in-txns (map (fn [t] (let [ng (process-name t)]
                              (-> t (dissoc :out)
                                  (assoc :name (:name ng) :group (:group ng)))))
                     raw-in-txns)]
    (doall (map cache-name in-txns))
    (doall (map (partial add-transactions in-txns) (nc/get-cache-keys)))
    @nc/name-cache))

(defn prettify-payment-type [{:keys [type] :as txn}]
  (let [pretty-type (s/replace type " to" "")]
    (assoc txn :type pretty-type)))

(defn coalesce-one-payee [payee match-set replacement]
  (when (some match-set (list payee)) replacement))

(defn coalesce-payees [coalesce-map {:keys [desc] :as txn}]
  (let [replacement (->> (map (fn [m-s rep] (coalesce-one-payee desc m-s rep))
                              (vals coalesce-map)
                              (keys coalesce-map))
                         (filter identity)
                         first)]
    (if replacement (assoc txn :desc replacement) txn)))

(defn process-expenditure [coalesce-map transactions]
  (->> transactions
       (filter #(nil? (:in %)))
       (map prettify-payment-type)
       (map (partial coalesce-payees coalesce-map))))
