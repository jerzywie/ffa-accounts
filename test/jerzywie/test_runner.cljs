;; This test runner is intended to be run from the command line
(ns jerzywie.test-runner
  (:require
    ;; require all the namespaces that you want to test
   [jerzywie.ffa-accounts.ffa-accounts-test]
   [jerzywie.ffa-accounts.cache_test]
   [jerzywie.ffa-accounts.csv-async-test]
   [jerzywie.ffa-accounts.csv-test]
   [jerzywie.ffa-accounts.allocate-test]
   [jerzywie.ffa-accounts.allocate-async-test]
   [jerzywie.ffa-accounts.analyse-test]

   [cljs.test :as test]
   [figwheel.main.async-result :as async-result]
   [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 10000))


(defmethod test/report [:cljs.test/default :end-run-tests] [m]
  (if (test/successful? m)
    (async-result/send "Tests Passed!!")
    (async-result/send "Tests FAILED")))
