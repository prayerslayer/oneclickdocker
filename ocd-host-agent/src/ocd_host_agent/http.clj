(ns ocd-host-agent.http
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ocd-host-agent.docker-api :as docker])
  (:use ring.middleware.json
        ring.middleware.reload
        ring.middleware.stacktrace))

(defn to-collection
  [thing]
  (if-not (or (list? thing)
              (vector? thing))
    (vector thing)
    (into (vector) thing)))

(defn set-in-path
  [obj path]
  (let [value (last path)
        ks (drop-last path)]
    (assoc-in obj ks value)))

(defn keep-keys
  [paths thing]
  ; set value at path
  (reduce set-in-path
          {}
          ; take paths and append value to them
          (->> paths
              (map to-collection)
              (map (fn [x]
                     (conj x (get-in thing x)))))))

; TODO list port
(def get-containers
  (GET "/containers"
    []
    (->> (docker/list-containers)
         (map #(docker/list-container (:Id %)))
         (map #(assoc % :Image (docker/list-image (:Image %))
                        :Metadata (get-in % [:Config :Labels])
                        :Name (.substring (:Name %) 1) ; strip leading slash
                        :Tags (->> (docker/list-images)
                                   (filter (fn [i] (= (:Id i)
                                                      (:Image %))))
                                   first
                                   :RepoTags)
                        :Port (get-in % [:HostConfig
                                         :PortBindings
                                         (keyword (str docker/DEFAULT_CONTAINER_PORT)
                                                  "tcp")
                                         0
                                         :HostPort
                                         (Integer/parseInt)])))
         (map (partial keep-keys [:Id
                                  :Created
                                  :Name
                                  :Metadata
                                  :Tags
                                  :Port
                                  :State
                                  [:Image :Id]
                                  [:Image :Parent]]))
         )))

; GET /containers ; for monitoring
;   >> id, port, user, status, image, health info von container (CPU/MEMORY/DISK)
; POST /stop
;   >> stop container x of user y
; POST /start
;   >> start container x of user y

(defroutes rest-api
  get-containers)

(def main
  (-> rest-api
      (wrap-reload)
      ;(wrap-stacktrace)
      (wrap-json-response)))