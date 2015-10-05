(ns ocd-host-agent.docker-api
  (:require [clojure.string :as str]
            [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(def not-nil?
  (comp not nil?)) 

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
   "Image" "c833a1892a15"
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

(println (json/encode default-container-config))

(def find-first
  (comp first filter))


(defn list-images
  [])

(defn pull
  [repository])

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
  (try
    (let [request (curl/post "http://127.0.0.1:4243/containers/create"
                             {:body (json/encode default-container-config)
                              :content-type :json})]
      (when (= 201 (:status request))
        (json/decode (:body request) true)))
    (catch Exception e
      (println (pr-str (ex-data e))))))

; (defn stop-container
;   [container]
;   (-> (.stopContainerCmd client (:id container))
;       (.exec)))

; (defn kill-container
;   [container]
;   (-> (.killContainerCmd client (:id container))
;       (.exec)))

(defn start-container
  [container]
  {:pre [(not-nil? container)]}
  (try
    (let [req (curl/post (str "http://127.0.0.1:4243/containers/" (:Id container) "/start"))]
      (when (= 204 (:status req))
        (println "YEAH")))
    (catch Exception e
      (println (pr-str (ex-data e))))))

(defn run-container
  [repository]
  ; TODO check if it's there, but stopped
  (-> repository
      (create-container)
      (start-container)))
