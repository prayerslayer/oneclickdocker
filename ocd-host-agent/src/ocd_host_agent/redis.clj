(ns ocd-host-agent.redis
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [overtone.at-at :as at]
            [environ.core :refer [env]]
            [ocd-host-agent.docker-api :as docker]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as redis]))

(def at-pool (at/mk-pool))

(def minute (* 1000 60))

(def hour (* 1000 60 60))

(def run-container-list
  "run-container-list")

(def redis-host
  (or (env :redis-host) "127.0.0.1"))

(def redis-port
  (or (env :redis-port) 6379))

(def redis-conn {:pool {}
                 :spec {:host redis-host
                        :port redis-port}})

(defmacro wcar*
  [& body]
  `(redis/wcar redis-conn ~@body))

(defn deconstruct-image
  [image]
  (let [[repo tag] (str/split image #":")
        tag (or tag "latest")]
    [repo tag]))

(defn poll-redis
  []
  (println "polling redis")
  (let [run-image (wcar* (redis/rpop run-container-list))]
    (when run-image
      (println "Found image " run-image " to run")
      (apply docker/run-container (deconstruct-image run-image)))))

(defrecord Redis
           [host port]
           component/Lifecycle
  (start [component]
    (println "starting redis client" (pr-str redis-conn))
    (at/every minute
              poll-redis
              at-pool
              :initial-delay (int (rand-int minute)))
    (assoc component :redis redis-conn))

  (stop [component]
    (println "stopping redis client")
    (at/stop *1)
    (assoc component :redis nil)))

(defn new-redis
  [host port]
  (map->Redis {:host host
               :port port}))