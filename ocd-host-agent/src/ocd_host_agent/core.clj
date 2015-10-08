(ns ocd-host-agent.core
  (:require [ocd-host-agent.redis :as redis]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn- run
  []
  (let [system (component/system-map :redis (redis/new-redis (env :redis-host)
                                                             (env :redis-port)))]
    (component/start system)))
  

(defn -main
  []
  (try
    (run)
    (catch Exception ex
      (println (pr-str ex)))))