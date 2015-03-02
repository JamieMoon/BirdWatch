(ns birdwatch-tc.interop.component
  (:gen-class)
  (:require
   [birdwatch-tc.interop.redis :as red]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [clojure.core.async :refer [chan]]))

;;; The interop component allows sending and receiving messages via Redis Pub/Sub.
;;; It has both a :send and a :receive channel and can be used on both sides of the Pub/Sub.
(defrecord Interop [conf channels]
  component/Lifecycle
  (start [component] (log/info "Starting Interop Component")
         (let [conn {:pool {} :spec {:host (:redis-host conf) :port (:redis-port conf)}}]
           (red/run-send-loop (:send channels) conn "matches")
           (assoc component :conn conn)))
  (stop  [component] (log/info "Stopping Interop Component") ;; TODO: proper teardown of resources
         (assoc component :conn nil)))

(defn new-interop [conf] (map->Interop {:conf conf}))

(defrecord Interop-Channels []
  component/Lifecycle
  (start [component] (log/info "Starting Interop Channels Component")
         (assoc component :send (chan) :receive (chan)))
  (stop  [component] (log/info "Stop Interop Channels Component")
         (assoc component :send nil :receive nil)))

(defn new-interop-channels [] (map->Interop-Channels {}))
