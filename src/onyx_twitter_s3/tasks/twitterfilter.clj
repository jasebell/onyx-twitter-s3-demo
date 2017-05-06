(ns onyx-twitter-s3.tasks.twitterfilter
  (:require [schema.core :as s]))

(defn get-data [fn-data]
  (if (.contains (:text fn-data) "#fashion")
    (do (println (str "*** Found match *** " (:text fn-data)))
        fn-data)))

(s/defn twitter-filter
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/type :function
                             :onyx/fn ::get-data}
                            task-opts)}}))
