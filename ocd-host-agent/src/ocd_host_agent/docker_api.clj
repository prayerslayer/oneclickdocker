(ns ocd-host-agent.docker-api
  (:require [clj-http.lite.client :as curl]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [cheshire.core :as json]))

(def base-url
  (or (env :docker-url)
      "http://127.0.0.1:4243"))

(defmacro return-on-success
  [req]
  `(try
     (let [request# ~req
           status# (:status request#)]
       (println "STATUS" status#)
       (if (and (>= status# 200)
                (< status# 300))
         (if (seq (:body request#))
           (json/decode (:body request#) true)
           true)
         (println "WARN: Status" status# "was returned." (:body request#))))
     (catch Exception e#
       (println (pr-str (ex-data e#))))))

(defn url
  [& path]
  (str base-url (apply str path)))

(def DEFAULT_CONTAINER_CONFIG
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
   "Cmd" ["/bin/sh" "-c" "./start.sh"] ; TODO login to docker registry, read image
   ; "Entrypoint" "" ; this too
   "Image" ""
   "Labels" {}
   ; "Mounts" []
   "WorkingDir" "/"
   "NetworkDisabled" false
   ; "MacAddress" "12:34:56:78:9a:bc" ; seriously
   "ExposedPorts" { "80/tcp" {} }
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
                 "PortBindings" {"22/tcp" [{ "HostPort" "22"}] }
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

(def DEFAULT_CONTAINER_PORT 80)
(def MINIMUM_HOST_PORT
  (or (env :docker-min-host-port)
      10000))

(defn list-images
  []
  (return-on-success (curl/get (url "/images/json")
                               {:query-params {"all" true}})))

(defn list-image
  [id]
  {:pre (some? id)}
  (return-on-success (curl/get (url "/images/" id "/json"))))

(defn list-container
  [id]
  {:pre (some? id)}
  (return-on-success (curl/get (url "/containers/" id "/json"))))

(defn list-containers
  []
  (return-on-success (curl/get (url "/containers/json")
                               {:query-params {"all" true}})))

(defn get-image
  [repository & [tag]]
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
  [repository & [tag]]
  (let [tag (or tag "latest")
        local-images (list-images)
        tags (flatten (map :RepoTags local-images))]
    (println "Checking if " repository ":" tag "in" (pr-str tags))
    (->> tags
         (some #(= % (str repository ":" tag)))
         (boolean))))

(defn pull
  [repository & [tag]]
  (let [tag (or tag "latest")]
    (println (str "Pulling image " repository ":" tag))
    (return-on-success (curl/post (url "/images/create")
                                  {:query-params {"fromImage" repository
                                                  "tag" tag}}))))

(defn- lowest-fitting-number
  "Returns lowest number that fits in between numbers.
   E.g. for 9 5 2 7 that would be 3."
  [numbers & [min-value]]
  (let [find-lowest (fn [a b]
                      (if (> (Math/abs (- a b))
                             1)
                        (-> (Math/min a b)
                            inc
                            reduced) ; stops reduction
                        b))
        numbers (sort numbers)
        port (reduce find-lowest
                     (or min-value 0)
                     numbers)]
    (if (= port (last numbers))
      ; no free port found
      (inc port)
      ; free port found
      port)))

(defn- get-used-ports
  "Returns used host ports based on container configs"
  []
  (->> (list-containers)
       (map #(list-container (:Id %)))
       (map :HostConfig)
       (map :PortBindings)
       (mapcat vals)
       (mapcat identity)
       (map #(get % :HostPort))
       (map #(Integer/parseInt %))
       (set)
       (sort)))

(defn- get-unused-host-port-config
  "Returns HostConfig with default container port bound to unused host port"
  []
  (let [port (lowest-fitting-number (get-used-ports)
                                    MINIMUM_HOST_PORT)]
    {"HostConfig" {"PortBindings" {(str DEFAULT_CONTAINER_PORT "/tcp") [{ "HostPort" (str port)}] }}}))


(defn create-container
  [repository & [tag config]]
  (println (str "Creating container from " repository ":" tag))
  (when-not (downloaded? repository tag)
    (pull repository tag))
  (let [image (get-image repository tag)
        config (merge DEFAULT_CONTAINER_CONFIG
                      (get-unused-host-port-config)
                      config
                      {"Image" (:Id image)})]
    (return-on-success (curl/post (url "/containers/create")
                                  {:body (json/encode config)
                                   :content-type :json}))))

(defn stop-container
  [container]
  {:pre [(some? container)]}
  (println (str "Stopping container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/stop")))
    (println "Stop OK")
    container))

(defn restart-container
  [container]
  {:pre [(some? container)]}
  (println (str "Restarting container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/restart")))
    (println "Restart OK")
    container))

(defn kill-container
  [container]
  {:pre [(some? container)]}
  (println (str "Killing container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/kill")))
    (println "Kill OK")
    container))

(defn delete-container
  [container]
  {:pre [(some? container)]}
  (println (str "Deleting container " (:Id container)))
  (when (return-on-success (curl/delete (url "/containers/"
                                             (:Id container))
                                        {:query-params {"force" true}}))
    (println "Delete OK")
    true))

(defn start-container
  [container]
  {:pre [(some? container)]}
  (println (str "Starting container " (:Id container)))
  (when (return-on-success (curl/post (url "/containers/"
                                           (:Id container)
                                           "/start")))
    (println "Start OK")
    container))

(defn run-container
  [repository & [tag config]]
  ; check if container is there but stopped
  (let [tag (or tag "latest")
        containers (list-containers)
        container (some #(when (= (:Image %)
                                  (str repository ":" tag))
                          %)
                        containers)]
    (if (some? container)
      (start-container container)
      (-> repository
          (create-container tag config)
          (start-container)))))
