(ns jerzywie.ffa-accounts.cache)

(def empty-name-cache {})

(def name-cache (atom empty-name-cache))

(defn empty-cache []
  (reset! name-cache empty-name-cache))

(defn cache! [k v]
  (swap! name-cache assoc k v))

(defn get-cache-value
  ([k default]
   (get @name-cache k default))
  ([k]
   (get-cache-value k nil)))

(defn get-cache-keys []
  (keys @name-cache))
