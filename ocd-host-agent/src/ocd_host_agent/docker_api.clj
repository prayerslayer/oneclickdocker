(ns ocd-host-agent.docker-api
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

; implementation of docker remote api
; https://docs.docker.com/reference/api/docker_remote_api_v1.20/

(defn to-env-list
  "Converts key-value map to list of key=value strings"
  [])

(def default-container-config
  {"Hostname" ""
   "Domainname" ""
   "User" ""
   "AttachStdout" true
   "AttachStderr" true
   "AttachStdin" false
   "Tty" false
   "OpenStdin" false
   "StdinOnce" false
   "Env" []
   "Cmd" ["date"] ; shouldn't this be obvious from the dockerfile
   "Entrypoint" "" ; this too
   "Image" "ubuntu"
   "Labels" {}
   "Mounts" []
   "WorkingDir" ""
   "NetworkDisabled" false
   "MacAddress" "12:34:56:78:9a:bc" ; seriously
   "ExposedPorts" { "8080/tcp" {} }
   "HostConfig" {"Binds" []
                 "Links" []
                 ; what is this
                 ; "LxcConf" {"lxc.utsname" "docker"}
                 ; "Memory" 0
                 ; "MemorySwap" 0
                 ; "CpuShares" 512
                 ; "CpuPeriod" 100000
                 ; "CpusetCpus" "01"
                 ; "CpusetMems" "01"
                 ; "BlkioWeight" 300
                 ; "MemorySwappiness" 60
                 ; "OomKillDisable" false
                 "PortBindings" {"8080/tcp" [{ "HostPort" "8080" }]
                                 "22/tcp" [{ "HostPort" "22"}] }
                 "PublishAllPorts" false
                 "Privileged" false
                 "ReadonlyRootfs" false
                 "Dns" ["8.8.8.8"]
                 "DnsSearch" [""]
                 "ExtraHosts" nil
                 "VolumesFrom" []
                 "CapAdd" ["NET_ADMIN"]
                 "CapDrop" ["MKNOD"]
                 "RestartPolicy" {"Name" ""
                                  "MaximumRetryCount" 0 }
                 "NetworkMode" "bridge"
                 "Devices" []
                 "Ulimits" [{}]
                 "LogConfig" {"Type" "json-file"
                              "Config" {} }
                 "SecurityOpt" []
                 "CgroupParent" ""}})

(defn create-container
  "Returns an Id if OK, nil otherwise"
  [url image]
  (let [config (-> (merge default-container-config
                          {"Image" image})
                   (json/encode))]
    (try
      (log/info config)
      (let [result (curl/post (str url "/containers/create")
                              {:body config
                               :content-type :json})]
        (when (= 201 (:status result))
          ; TODO print warnings
          (-> result
              (:body)
              (json/decode)
              (get "Id"))))
      (catch Exception ex
        (pr-str ex)))))

(defn start-container
  "Returns status or nil on exception"
  [url container-id]
  (try
    (-> (curl/post (str url "/containers/" container-id "/start"))
        (:status))
    (catch Exception ex
      (pr-str ex))))

; ====

; this be public

(defn run-container
  "Accepts a tag of an image and a name for the container to start"
  [url tag env name]
  (-> (create-container url tag)
      (start-container)))

(defn stop-container
  "Stops the container"
  [url name])

(defn list-images
  [url]
  (->> (str url "/images/json")
       (curl/get)
       (:body)
       (json/decode)
       (log/spyf "Docker images: %s")))

(defn list-containers
  [url]
  (->> (str url "/containers/json")
       (curl/get)
       (:body)
       (json/decode)))