(ns ocd-host-agent.http
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as ring]
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
                        :Port (-> (get-in % [:HostConfig
                                             :PortBindings
                                             (keyword (str docker/DEFAULT_CONTAINER_PORT)
                                                      "tcp")
                                             0
                                             :HostPort])
                                  (Integer/parseInt))))
         (map (partial keep-keys [:Id
                                  :Created
                                  :Name
                                  :Metadata
                                  :Tags
                                  :Port
                                  :State
                                  [:Image :Id]
                                  [:Image :Parent]])))))

(def alter-container-status
  (POST "/containers"
    {body :body}
    (let [known? #{"start" "stop" "kill"}]
      (if (or (not (known? (:command body)))
              (nil? (:container body))
              (nil? (:user body)))
        {:status 400}
        (let [cmd (:command body)
              user (:user body)
              container-id (:container body)
              containers (->> (docker/list-containers)
                              (filter #(= container-id (:Id %))))]
          (if (empty? containers)
            (ring/not-found (str "No container with id " container-id))
            (let [container (docker/list-container (:Id (first containers)))]
              (if (= user
                     (get-in container [:Config :Labels :user]))
                (case (.toLowerCase cmd)
                  "start" (do
                            (docker/start-container (first container))
                            (ring/response nil))
                  "stop" (do
                           (docker/stop-container (first container))
                           (ring/response nil))
                  "kill" (do
                           (docker/kill-container (first container))
                           (ring/response nil)))
                (ring/not-found "No such container of this user.")))))))))

; GET /containers ; for monitoring
;   >> id, port, user, status, image, health info von container (CPU/MEMORY/DISK)
; POST /stop
;   >> stop container x of user y
; POST /start
;   >> start container x of user y

(defroutes rest-api
  get-containers
  alter-container-status)

(def main
  (-> rest-api
      (wrap-reload)
      ;(wrap-stacktrace)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))