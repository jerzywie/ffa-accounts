(ns jerzywie.ffa-accounts.test-util
  (:require [jerzywie.ffa-accounts.cache :as cache]
            [goog.dom :as gdom]
            [goog.labs.format.csv :as csv]
            [cljs.core.async :as async :refer [put! chan <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn start-with-empty-cache [f]
  (cache/empty-cache)
  (f))

(defn dbg-> [v msg]
  (prn "dbg-> " msg v)
  v)

(def extract-result
  (map #(-> %
            .-target
            .-result
            csv/parse
            js->clj)))


(def input-elements (chan 1))
(def file-reads (chan 1 extract-result))

(defn request-input-element-value [el-name]
  (put! input-elements el-name))

(defn get-input-element [el-name]
  (gdom/getElement el-name))

(defn get-file-from-input [el]
  (-> el .-files (aget 0)))

(defn read-file [file]
  (let [reader (js/FileReader.)]
    (.readAsText reader file)))

(go-loop []
  (let [input-el (get-input-element (<! input-elements))
        file (get-file-from-input input-el)
        reader (js/FileReader.)]
    (set! (.-onload reader) #(put! file-reads %))
    (.readAsText reader file)
    (recur)))
