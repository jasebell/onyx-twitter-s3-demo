(ns onyx-twitter-s3.jobs.basic-test
    (:require [aero.core :refer [read-config]]
              [clojure.core.async :refer [>!!]]
              [clojure.java.io :as io]
              [clojure.test :refer [deftest is testing]]
              [onyx api
               [test-helper :refer [with-test-env]]]
              [onyx.plugin.core-async :refer [get-core-async-channels take-segments!]]
              onyx-twitter-s3.jobs.basic
              ;; Include function definitions
              onyx-twitter-s3.tasks.math
              onyx.tasks.core-async))

(def segments [{:n 1} {:n 2} {:n 3} {:n 4} {:n 5} :done])

(deftest basic-test
  (testing "That we can have a basic in-out workflow run through Onyx"
    (let [{:keys [env-config
                  peer-config]} (read-config (io/resource "config.edn"))
          job (onyx-twitter-s3.jobs.basic/basic-job {:onyx/batch-size 10
                                                  :onyx/batch-timeout 1000})
          {:keys [in out]} (get-core-async-channels job)]
      (with-test-env [test-env [3 env-config peer-config]]
        (onyx.test-helper/validate-enough-peers! test-env job)
        (let [job (onyx.api/submit-job peer-config job)]
          (is (:success? job))
          (doseq [segment segments]
            (>!! in segment))
          (onyx.test-helper/feedback-exception! peer-config (:job-id job)))
        (is (= (set (take-segments! out))
               (set [{:n 2} {:n 3} {:n 4} {:n 5} {:n 6} :done])))))))
