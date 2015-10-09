(ns ocd-host-agent.redis
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [overtone.at-at :as at]
            [cheshire.core :as json]
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

(defn parse-image
  [image]
  (let [[repo tag] (str/split image #":")
        tag (or tag "latest")]
    [repo tag]))

(defn hmap->clj
  [hmap]
  (when-not (nil? hmap)
    (->> hmap
         (partition 2)
         (map #(assoc {} (keyword (first %))
                         (second %)))
         (reduce merge))))

(defn poll-redis
  []
  (println "polling redis")
  (let [task-id (wcar* (redis/rpop run-container-list))]
    (when task-id
      (println "Found task " task-id " to run")
      (let [task (hmap->clj (wcar* (redis/hgetall task-id)))]
        (when task
          (apply docker/run-container (conj (parse-image (:image task))
                                            {"Labels" task})))))))

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