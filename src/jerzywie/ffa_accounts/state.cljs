(ns jerzywie.ffa-accounts.state
  (:require[reagent.core :as reagent :refer [atom]] ))

;;define app state
(defonce initial-app-state {})
(defonce app-state (atom initial-app-state))

(defn reset-state! []
  (reset! app-state initial-app-state))
