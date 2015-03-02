(ns birdwatch-tc.persistence.elastic
  (:gen-class)
  (:require
   [clojure.tools.logging :as log]
   [clojurewerkz.elastisch.rest.document :as esd]
   [clojure.core.async :refer [<! chan put! timeout go-loop]]))

(defn run-persistence-loop
  "run loop for persisting tweets"
  [persistence-chan conf conn]
  (go-loop [] (let [t (<! persistence-chan)]
                (try
                  (esd/put conn (:es-index conf) "tweet" (:id_str t) t)
                  (catch Exception ex (log/error ex "esd/put error"))))
           (recur)))
