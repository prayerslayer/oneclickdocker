(ns ocd-host-agent.docker-api-test
  (:require [clojure.test :refer :all]
            [ocd-host-agent.docker-api :as docker]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [ocd-host-agent.docker-api :as docker]))

(defmacro same!
  [x y]
  `(is (= ~x ~y)))

(defmacro not-same!
  [x y]
  `(is (not (= ~x ~y))))

(defmacro false!
  [x]
  `(same! false ~x))

(defmacro true!
  [x]
  `(same! true ~x))

; TODO proper integration test with docker daemon
(def TESTREPO "prayerslayer/ocd-hello-world")
(def TESTTAG "prayerslayer/ocd-hello-world:latest")

(defn- list-containers
  []
  (let [cmd (sh "docker" "ps" "-aq")]
    (filter seq
            (str/split (:out cmd) #"\n"))))

(defn- delete-containers!
  []
  (let [containers (list-containers)]
    (when (seq containers)
      (println "Deleting all containers")
      (pr-str (apply sh (into ["docker" "rm" "-f"] containers))))))

(defn- delete-images!
  []
  (let [cmd (sh "docker" "images" "-q")
        images (filter seq
                       (str/split (:out cmd) #"\n"))]
    (when (seq images)
      (println "Deleting all images")
      (pr-str (apply sh (into ["docker" "rmi" "-f"] images))))))

(defn- wipe!
  []
  (delete-containers!)
  (delete-images!))

; check that docker works
(deftest test-docker-cli
  (let [cmd (sh "docker")]
    (same! (:exit cmd) 0)))

(deftest test-no-images
  (wipe!)
  
  (true! (nil? (docker/get-image TESTREPO)))
  (false! (docker/downloaded? TESTREPO))
  (true! (nil? (docker/get-image TESTREPO "latest")))
  (false! (docker/downloaded? TESTREPO "latest"))
  (true! (nil? (docker/get-image "busybox")))
  (false! (docker/downloaded? "busybox"))
  (true! (empty? (docker/list-images))))

(deftest test-pull
  (wipe!)

  (docker/pull TESTREPO)
  (true! (docker/downloaded? TESTREPO))
  (not-same! (docker/get-image TESTREPO)
             nil)
  (same! (first (:RepoTags (docker/get-image TESTREPO)))
         TESTTAG))

(deftest test-create-without-image
  (wipe!)
  
  (let [container (docker/create-container TESTREPO)]
    (not-same! nil container)
    (same! 1 (count (list-containers)))))

(deftest test-create-with-image
  (wipe!)
  
  (docker/pull TESTREPO)
  (let [container (docker/create-container TESTREPO)]
    (not-same! nil container)
    (same! 1 (count (list-containers)))))

(deftest test-run-without-container
  (wipe!)
  
  (let [running (docker/run-container TESTREPO)]
    ; sleep 2s to allow nginx to boot
    (Thread/sleep 2000)
    (not-same! nil running)
    (same! 1 (count (list-containers)))
    (let [cmd (sh "curl" "127.0.0.1:8080")]
      (println (:out cmd))
      (same! 0 (:exit cmd)))))

(deftest test-run-with-container
  (wipe!)
  
  (let [container (docker/create-container TESTREPO)
        running (docker/run-container TESTREPO)]
    ; TODO not really testing that it ran or is running
    (not-same! nil running)
    (same! 2 (count (list-containers)))))

(deftest test-delete-container
  (wipe!)

  (let [container (docker/create-container TESTREPO)]
    (true! (docker/delete-container container))
    (same! 0 (count (list-containers)))))

(deftest test-delete-running-container
  (wipe!)
  
  (let [running (docker/run-container TESTREPO)]
    (Thread/sleep 2000)
    ; sleep 2s to allow nginx to boot
    (docker/delete-container running)
    (Thread/sleep 2000)
    (let [cmd (sh "curl" "127.0.0.1:8080")]
      (println (:out cmd))
      (not-same! 0 (:exit cmd)))))

(deftest test-kill-container
  (wipe!)
  
  (let [running (docker/run-container TESTREPO)]
    (Thread/sleep 2000)
    ; sleep 2s to allow nginx to boot
    (docker/kill-container running)
    (Thread/sleep 2000)
    (let [cmd (sh "curl" "127.0.0.1:8080")]
      (println (:out cmd))
      (not-same! 0 (:exit cmd)))))