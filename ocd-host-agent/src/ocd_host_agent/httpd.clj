(ns ocd-host-agent.httpd
  (:require [ocd-host-agent.http :as handler]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defrecord HttpServer
           [port]
           component/Lifecycle

  (start [component]
         (let [server (run-jetty handler/main
                                 {:port port})]
           (log/info "Running Jetty on " port)
           (assoc component :http server)))

  (stop [component]
        (do 
          (log/info "Stopping Jetty...")
          (.stop (:http component))
          (assoc component :http nil))))

(defn new-httpd [port]
  (map->HttpServer {:port port}))