(ns ocd-host-agent.redis
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [taoensso.carmine :as redis :refer (wcar)]
            [com.stuartsierra.component :as component]))

(defrecord Redis
           [host port]
           component/Lifecycle
  (start [component]
    (assoc component :redis {:pool {}
                             :spec {:host host
                                    :port port}}))
  (stop [component]
    (assoc component :redis nil)))

(defn new-redis
  [host port]
  (map->Redis {:host host
               :port port}))