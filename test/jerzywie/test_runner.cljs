;; This test runner is intended to be run from the command line
(ns jerzywie.test-runner
  (:require
    ;; require all the namespaces that you want to test
    [jerzywie.ffa-accounts-test]
    [jerzywie.cache_test]
    [jerzywie.csv-async-test]
    [jerzywie.csv-test]
    [figwheel.main.testing :refer [run-tests-async]]
    [figwheel.main.async-result :as async-result]
    [cljs.test :as test]))

(defn -main [& args]
  (run-tests-async 5000))

;(defmethod test/report [:cljs.test/default :end-run-tests] [m]
;  (if (test/successful? m)
;    (async-result/send "Tests Passed!!")
;    (async-result/send "Tests FAILED")))
