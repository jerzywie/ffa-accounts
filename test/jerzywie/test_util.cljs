(ns jerzywie.test-util
  (:require [jerzywie.cache :as cache]))

(defn start-with-empty-cache [f]
  (cache/empty-cache)
  (f))
