(ns ocd-host-agent.redis
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [overtone.at-at :as at]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [ocd-host-agent.docker-api :as docker]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as redis]))

(def REDIS_POOL (at/mk-pool))

(def ONE_SECOND 1000)

(def ONE_MINUTE (* ONE_SECOND 60))

(def ONE_HOUR (* ONE_MINUTE 60))

(def TASK_QUEUE
  "TQUEUE")

(def HOST_NAME
  (or (env :host-name)
      "donkey"))

(def REDIS_HOST
  (or (env :redis-host)
      "127.0.0.1"))

(def REDIS_POST
  (or (env :redis-port) 6379))

(def REDIS_CONN {:pool {}
                 :spec {:host REDIS_HOST
                        :port REDIS_POST}})

(defmacro wcar*
  [& body]
  `(redis/wcar REDIS_CONN ~@body))

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
         (map #(assoc {} (first %)
                         (second %)))
         (reduce merge))))

(defn poll-redis
  []
  (println "polling redis")
  (let [task-id (wcar* (redis/rpop TASK_QUEUE))]
    (when task-id
      (println "Found task " task-id " to run")
      (let [task (hmap->clj (wcar* (redis/hgetall task-id)))]
        (when task
          (apply docker/run-container (conj (parse-image (get task "image"))
                                            {"Labels" task})))))))

(defn analyze-status
  [status]
  (if (:Running status)
    {:Status "Running"
     :Time (:StartedAt status)}
    (if (:Restarting status)
      {:Status "Restarting"
       :Time (:StartedAt status)}
      (if (:Paused status)
        {:Status "Paused"
         :Time (:StartedAt status)}
        {:Status "Stopped"
         :Time (:FinishedAt status)}))))

(defn delete-container
  [user container-id]
  (let [key (str "CNT;" user ";" container-id)]
    (docker/delete-container {:Id container-id})
    (wcar* (redis/srem (str "USR;" user) key))
    (wcar* (redis/del key))))

(defn gargabe-collect
  "Delete containers stopped for more than 7 days"
  []
  (println "collecting garbage")
  (let [containers (docker/list-containers)]
    (doseq [container containers]
      (let [container-id (:Id container)
            user (get-in [:Metadata :user] container)
            status (analyze-status (:State container))
            time (tf/parse (tf/formatters :date-time) (:Time status))]
        (println container-id (pr-str status))
        (when (= "Stopped" (:Status status))
          (when-not (t/within? (-> 1 t/weeks t/ago)
                               (t/now)
                               time)
            (println "deleting container" container-id)
            (delete-container user container-id)))))))

(defn push-redis
  []
  (println "pushing data to redis")
  (let [containers (docker/list-containers)]
    ; update container information
    (doseq [container containers]
      (let [container-id (:Id container)
            user (get-in container [:Metadata :user])
            key (str "CNT;" user ";" container-id)
            status (analyze-status (:State container))
            time (tf/parse (tf/formatters :date-time) (:Time status))]
        ; when container is stopped and older than a week, stop writing updates
        ; so the garbage collector can remove it
        (when-not (and (= "Stopped" (:Status status))
                       (t/within? (-> 1 t/weeks t/ago)
                                  (t/now)
                                  time))
          (println "writing to" key)
          (wcar* (redis/sadd (str "USR;" user) container-id))
          (wcar* (redis/hmset key "port" (:Port container)
                                  "host" HOST_NAME
                                  "created_at" (:Created container)
                                  "name" (:Name container)
                                  "id" container-id
                                  "image" (:Id (:Image container))
                                  "tag" (first (:Tags container))
                                  "status" (.toLowerCase (:Status status))
                                  (if (= "Stopped"
                                         (:Status status))
                                    "finished_at"
                                    "started_at")
                                  (:Time status))))))))

(defrecord Redis
           []
           component/Lifecycle
  (start [component]
    (println "starting redis client" (pr-str REDIS_CONN))
    (at/every ONE_MINUTE
              poll-redis
              REDIS_POOL
              :initial-delay (int (rand-int ONE_MINUTE)))
    (at/every (* 10 ONE_SECOND)
              push-redis
              REDIS_POOL)
    (at/every ONE_MINUTE
              gargabe-collect
              REDIS_POOL)
    (assoc component :redis REDIS_CONN))

  (stop [component]
    (println "stopping redis client")
    (at/stop *1)
    (assoc component :redis nil)))

(defn new-redis
  []
  (map->Redis {}))