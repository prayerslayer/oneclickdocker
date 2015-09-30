(ns ocd-host-agent.docker-api
  (:import [com.github.dockerjava.core DockerClientConfig]
           [com.github.dockerjava.api.model ExposedPort
                                            Ports]))

(def client
  (-> (DockerClientConfig/createDefaultConfigBuilder)
      (.withUri "http://192.168.59.103:2375")
      (.build)))

(defn get-default-exposed-ports
  []
  [ssh (ExposedPort/tcp 22)
   web (ExposedPort/tcp 8080)])

(defn get-default-port-bindings
  [ssh web]
  (-> (Ports.)
      (.bind ssh (Ports/Binding 11122))
      (.bind web (Ports/Binding 8080))))

(defn create-container
  [image]
  (let [exposed (get-default-exposed-ports)
        ssh (first exposed)
        web (second exposed)]
    (-> (.createContainerCmd client image)
        (.withCmd "date")
        (.withExposedPorts ssh web)
        (.withPortBindings (get-default-port-bindings (get-default-port-bindings ssh web)))
        (.exec))))

(defn start-container
  [container]
  (-> (.startContainerCmd client (.getId container))
      (.exec)))

(defn run-container
  [image]
  (-> image
      (create-container)
      (start-container)))
