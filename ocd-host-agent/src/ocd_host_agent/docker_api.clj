(ns ocd-host-agent.docker-api
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [cheshire.core :as json]))

(def base-url
  (or (env :docker-url)
      "http://127.0.0.1:4243"))

(def not-nil?
  (comp not nil?))

(def find-first
  (comp first filter))

(defmacro return-on-success
  [req]
  `(try
     (let [request# ~req
           status# (:status request#)]
       (when (and (>= status# 200)
                  (< status# 300))
         (if (some? (:body request#))
           (json/decode (:body request#) true)
           true)))
     (catch Exception e#
       (log/debug (pr-str (ex-data e#))))))

(defn url
  [& path]
  (str base-url (apply str path)))

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
                 ; "ReadonlyRootfs" false
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


(defn list-images
  []
  (return-on-success (curl/get (url "/images/json")
                               {:query-params {"all" true}})))

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
  (log/debug (str "Pulling image " repository ":" tag))
  (return-on-success (curl/post (url "/images/create")
                                {:query-params {"fromImage" repository
                                                "tag" (or tag "latest")}})))

(defn create-container
  [repository tag]
  (log/debug (str "Creating container from " repository ":" tag))
  (when-not (downloaded? repository tag)
    (pull repository tag))
  (let [image (get-image repository tag)
        config (merge default-container-config {"Image" (:Id image)})]
    (return-on-success (curl/post (url "/containers/create")
                                  {:body (json/encode config)
                                   :content-type :json}))))

(defn list-containers
  []
  (return-on-success (curl/get "http://127.0.0.1:4243/containers/json"
                               {:query-params {"all" true}})))

(defn stop-container
  [container]
  {:pre [(not-nil? container)]}
  (log/debug (str "Stopping container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/stop")))
    container))

(defn restart-container
  [container]
  {:pre [(not-nil? container)]}
  (log/debug (str "Restarting container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/restart")))
    container))

(defn kill-container
  [container]
  {:pre [(not-nil? container)]}
  (log/debug (str "Killing container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/kill")))
    container))

(defn start-container
  [container]
  {:pre [(not-nil? container)]}
  (log/debug (str "Starting container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/start")))
    (log/debug "Start OK")
    container))

(defn run-container
  [repository tag]
  ; check if container is there but stopped
  (let [tag (or tag "latest")
        containers (list-containers)
        container (find-first #(= (:Image %)
                                  (str repository ":" tag))
                              containers)]
    (if (some? container)
      (start-container container)
      (-> repository
          (create-container tag)
          (start-container)))))
