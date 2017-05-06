(ns onyx-twitter-s3.shared)

(def serializer-fn (fn [vs]
                     (.getBytes (pr-str vs) "UTF-8")))
