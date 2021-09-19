(ns jerzywie.csv-file-test
  (:require [jerzywie.test-util :as util]
            [cljs.test :refer-macros [deftest async]]
            [cljs.core.async :as async :refer [go <!]]))

  (deftest file-tests-async
    (let [_ (util/request-input-element-value "inp")]
      (async done
             (go
               (let [result (<! util/file-reads)]
                 (prn "result" result)))
             (done))))
