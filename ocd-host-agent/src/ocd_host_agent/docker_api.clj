(ns ocd-host-agent.docker-api
  (:require [clojure.string :as str]
            [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(def not-nil?
  (comp not nil?)) 

(def default-container-config
  {
   ; "Hostname" ""
   ; "Domainname" ""
   ; "User" ""
   "AttachStdout" true
   "AttachStderr" true
   "AttachStdin" false
   "Tty" false
   "OpenStdin" false
   "StdinOnce" false
   "Env" []
   "Cmd" ["date"] ; shouldn't this be obvious from the dockerfile
   ; "Entrypoint" "" ; this too
   "Image" ""
   "Labels" {}
   ; "Mounts" []
   "WorkingDir" "/"
   "NetworkDisabled" false
   ; "MacAddress" "12:34:56:78:9a:bc" ; seriously
   "ExposedPorts" { "8080/tcp" {} }
   "HostConfig" {
                 ; "Binds" []
                 ; "Links" []
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
                 ; "DnsSearch" [""]
                 ; "ExtraHosts" nil
                 ; "VolumesFrom" []
                 ; "CapAdd" ["NET_ADMIN"]
                 ; "CapDrop" ["MKNOD"]
                 "RestartPolicy" {"Name" "NoRetry"
                                  "MaximumRetryCount" 0 }
                 "NetworkMode" "bridge"
                 ; "Devices" []
                 ; "Ulimits" [{}]
                 "LogConfig" {"Type" "json-file"
                              "Config" {} }
                 ; "SecurityOpt" []
                 ; "CgroupParent" ""
                 }})

(println (json/encode default-container-config))

(def find-first
  (comp first filter))

(defn list-images
  []
  (try
    (let [request (curl/get "http://127.0.0.1:4243/images/json"
                            {:query-params {"all" true}})]
      (when (= 200 (:status request))
        (json/decode (:body request) true)))
    (catch Exception e
      (println (pr-str (ex-data e))))))

(defn get-image
  [repository tag]
  (let [tag (or tag "latest")
        images (list-images)]
    (->> images
         ; remove those with repo tag none
         (remove #(and (= 1 (count (:RepoTags %)))
                       (= "<none>:<none>" (first (:RepoTags %)))))
         ; first of those which contains correct tag
         (filter (fn [image]
                   (some #(= % (str repository ":" tag)) (:RepoTags image))))
         (first))))

(defn downloaded?
  [repository tag]
  (let [tag (or tag "latest")
        local-images (list-images)
        tags (flatten (map :RepoTags local-images))]
    (->> tags
         (some #(= % (str repository tag)))
         (boolean))))

(defn pull
  [repository tag]
  ; TODO wrap this up in if-success macro
  (try 
    (let [request (curl/post (str "http://127.0.0.1:4243/images/create")
                             {:query-params {"fromImage" repository
                                             "tag" (or tag "latest")}})]
      (when (= 200 (:status request))
        (json/decode (:body request))))
    (catch Exception e
      (println (pr-str (ex-data e))))))

(defn create-container
  [repository tag]
  (try
    (when-not (downloaded? repository tag)
      (pull repository tag))
    (let [image (get-image repository tag)
          config (merge default-container-config {"Image" (:Id image)})
          request (curl/post "http://127.0.0.1:4243/containers/create"
                             {:body (json/encode config)
                              :content-type :json})]
      (when (= 201 (:status request))
        (json/decode (:body request) true)))
    (catch Exception e
      (println (pr-str (ex-data e))))))

(defn list-containers
  []
  (try
    (let [request (curl/get "http://127.0.0.1:4243/containers/json"
                            {:query-params {"all" true}})]
      (when (= 200 (:status request))
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
