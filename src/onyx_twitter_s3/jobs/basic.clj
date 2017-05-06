(ns onyx-twitter-s3.jobs.basic
  (:require [onyx.job :refer [add-task register-job]]
            [onyx.tasks.core-async :as core-async-task]
            [onyx-twitter-s3.tasks.math :as math]
            [onyx-twitter-s3.tasks.twitterfilter :as filter]
            [environ.core :refer [env]]
            [onyx.plugin.twitter]
            [onyx.plugin.s3-output]
            [onyx.plugin.s3-utils :as s3utils]
            [onyx.tasks.twitter :as twitter]
            [onyx-twitter-s3.shared]
            [onyx.tasks.s3 :as s3]))

(def batch-size 5000)

(defn basic-job
  [batch-settings twitter-config s3-opts]
  (let [client (s3utils/set-region (s3utils/new-client) "eu-west-1")
        base-job {:workflow [[:in :identity]
                             [:identity :out]]
                  :catalog [{:onyx/name :filter
                             :onyx/fn :onyx-twitter-s3.tasks.twitterfilter/get-data
                             :onyx/type :function
                             :onyx/batch-size 20}
                            {:onyx/name :identity
                             :onyx/fn :clojure.core/identity
                             :onyx/type :function
                             :onyx/batch-size 20}]
                  :lifecycles []
                  :windows []
                  :triggers []
                  :flow-conditions []
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (-> base-job
        (add-task (twitter/stream :in :all (merge twitter-config batch-settings)))
        (add-task (s3/s3-output :out (merge s3-opts batch-settings))))))

(defmethod register-job "basic-job"
  [job-name config]
  (let [batch-settings {:onyx/batch-size batch-size :onyx/batch-timeout 10000}
        twitter-config {:twitter/consumer-key (env :twitter-consumer-key)
                        :twitter/consumer-secret (env :twitter-consumer-secret)
                        :twitter/access-token (env :twitter-access-token)
                        :twitter/access-secret (env :twitter-access-secret)
                        :twitter/keep-keys [:id :text]
			:twitter/track "#fashion"}
        s3-opts {:onyx/name :out
                 :onyx/plugin :onyx.plugin.s3-output/output
                 :s3/bucket "onyx-output"
                 :s3/encryption :none
                 :s3/serializer-fn :onyx-twitter-s3.shared/serializer-fn
                 :s3/key-naming-fn :onyx.plugin.s3-output/default-naming-fn
                 :onyx/type :output
                 :onyx/medium :s3
                 :onyx/batch-size batch-size
                 :onyx/doc "Writes segments to s3 files, one file per batch"}]
    (basic-job batch-settings twitter-config s3-opts)))
