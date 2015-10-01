(ns ocd-host-agent.docker-api
  (:import [com.github.dockerjava.core DockerClientConfig
                                       DockerClientBuilder]
           [com.github.dockerjava.jaxrs DockerCmdExecFactoryImpl]
           [com.github.dockerjava.api.model ExposedPort
                                            Ports]))

(defn get-client
  []
  (let [config (-> (DockerClientConfig/createDefaultConfigBuilder)
                   (.withUri "http://localhost:4342")
                   (.build))
        factory (-> (DockerCmdExecFactoryImpl.)
                    (.withReadTimeout (int 1000))
                    (.withWriteTimeout (int 1000)))]
    (-> (DockerClientBuilder/getInstance config)
        (.withDockerCmdExecFactory factory)
        (.build))))

(def client (get-client))

(defn get-default-exposed-ports
  []
  [(ExposedPort/tcp 22)
   (ExposedPort/tcp 8080)])

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
