(ns ocd-host-agent.redis
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as redis]))

(def at-pool (at/mk-pool))

(def hour (* 1000 60 60))

(def container-list
  "container-list")

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

(defn poll-redis
  []
  (println "polling redis")
  (let [image (wcar* (redis/rpop container-list))]
    (when image
      (println (str "Found image " image " in Redis")))))

(defrecord Redis
           [host port]
           component/Lifecycle
  (start [component]
    (println "starting redis client" (pr-str redis-conn))
    (at/every 1000
              poll-redis
              at-pool)
    (assoc component :redis redis-conn))

  (stop [component]
    (println "stopping redis client")
    (at/stop *1)
    (assoc component :redis nil)))

(defn new-redis
  [host port]
  (map->Redis {:host host
               :port port}))