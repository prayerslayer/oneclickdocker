(ns ocd-host-agent.docker-api
  (:require [clojure.string :as str])
  (:import [com.github.dockerjava.core DockerClientConfig
                                       DockerClientBuilder]
           [com.github.dockerjava.jaxrs DockerCmdExecFactoryImpl]
           [com.github.dockerjava.core.command PullImageResultCallback]
           [com.github.dockerjava.api.model ExposedPort
                                            Ports]))

(defn get-client
  []
  (let [config (-> (DockerClientConfig/createDefaultConfigBuilder)
                   (.withUri "http://0.0.0.0:4243")
                   (.build))]
    (-> (DockerClientBuilder/getInstance config)
        (.build))))

(def find-first
  (comp first filter))

(def client (get-client))

(defn get-default-exposed-ports
  []
  [(ExposedPort/parse "22/tcp")
   (ExposedPort/parse "8080/tcp")])

(defn get-default-port-bindings
  [ssh web]
  (let [ports (Ports.)]
    (.bind ports ssh (Ports/Binding (int 11122)))
    (.bind ports web (Ports/Binding (int 8080)))
    ports))

(defn image-to-clj
  [image]
  {:id (.getId image)
   :repoTags (->> (vec (aclone (.getRepoTags image)))
                  (filter #(not (= "<none>:<none>" %))))
   :created (.getCreated image)
   :size (.getSize image)
   :virtualSize (.getVirtualSize image)})

(defn list-images
  []
  (let [image-list (-> (.listImagesCmd client)
                       (.withShowAll true)
                       (.exec))]
    (map image-to-clj image-list)))

(defn pull
  [repository]
  ; dockerClient.pullImageCmd(testImage).exec(new PullImageResultCallback()).awaitSuccess();
  (-> (.pullImageCmd client repository)
      (.exec (PullImageResultCallback.))
      (.awaitSuccess)))

(defn downloaded?
  [repository]
  (let [[repo tag] (str/split repository #":")
        local-images (list-images)
        tags (flatten (map :repoTags local-images))]
    (->> tags
         (some #(= % (if-not tag
                             (str repo ":latest")
                             repository)))
         (boolean))))

(defn create-container
  [repository]
  (let [exposed (get-default-exposed-ports)
        ssh (first exposed)
        web (second exposed)
        local-images (list-images)]
    (when-not (downloaded? repository)
      (pull repository))
    (-> (.createContainerCmd client repository)
        ; this is how to handle variable length arguments from java in clojure
        ; http://stackoverflow.com/questions/11702184/how-to-handle-java-variable-length-arguments-in-clojure
        (.withCmd (into-array String ["./start.sh"]))
        (.withExposedPorts (into-array ExposedPort [ssh web]))
        (.withPortBindings (get-default-port-bindings ssh web))
        (.exec))))

(defn stop-container
  [container]
  (-> (.stopContainerCmd client (.getId container))
      (.exec)))

(defn kill-container
  [container]
  (-> (.killContainerCmd client (.getId container))
      (.exec)))

(defn start-container
  [container]
  (-> (.startContainerCmd client (.getId container))
      (.exec)))

(defn run-container
  [repository]
  ; TODO check if it's there, but stopped
  (-> repository
      (create-container)
      (start-container)))
