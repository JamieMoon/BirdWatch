(ns birdwatch.persistence.elastic
  (:gen-class)
  (:require
   [birdwatch.persistence.tools :as pt]
   [com.matthiasnehlsen.inspect :refer [inspect]]
   [clojure.tools.logging :as log]
   [clojurewerkz.elastisch.rest.document :as esd]
   [clojurewerkz.elastisch.rest.response :as esrsp]
   [clojure.core.async :refer [<! <!! chan put! timeout go-loop thread onto-chan]]))

(defn query
  "run a query on previous matching tweets"
  [{:keys [query n from]} conf conn]
  (let [search (esd/search conn (:es-index conf) "tweet" :query query :size n :from from :sort {:id :desc})
        hits (esrsp/hits-from search)
        source (pt/get-source hits)
        res (vec source)]
    res))

(defn query-xf
  "create transducer for answering queries"
  [conf conn]
  (map (fn [q]
         (inspect :elastic/query q)
         {:uid (:uid q) :result (query q conf conn)})))

(defn tweet-query-xf
  "create transducer for finding missing tweets"
  [conf conn]
  (map (fn [req]
         (let [res (esd/get conn (:es-index conf) "tweet" (:id_str req))]
           (inspect :elastic/missing {:req req :res res})
           (when-not res (log/debug "birdwatch.persistence missing" (:id_str req) res))
           {:tweet (pt/strip-source res) :uid (:uid req)}))))

(defn run-tweet-count-loop
  "run loop for sending stats about total number of indexed tweets"
  [tweet-count-chan conf conn]
  (go-loop [] (<! (timeout 10000))
           (let [cnt (esd/count conn (:es-index conf) "tweet")]
             (inspect :elastic/tweet-count cnt)
             (put! tweet-count-chan (format "%,15d" (:count cnt))))
           (recur)))

