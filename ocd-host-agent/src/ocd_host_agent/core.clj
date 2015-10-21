(ns ocd-host-agent.core
  (:require [ocd-host-agent.httpd :as httpd]
            [ocd-host-agent.redis :as redis]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(def http-port
  (or (env :http-port)
      8080))

(defn- run
  []
  (let [system (component/system-map :redis (redis/new-redis)
                                     :httpd (httpd/new-httpd http-port))]
    (component/start system)))


(defn -main
  []
  (try
    (run)
    (catch Exception ex
      (println (pr-str ex)))))