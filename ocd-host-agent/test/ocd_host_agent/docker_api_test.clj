(ns ocd-host-agent.docker-api-test
  (:require [clojure.test :refer :all]
            [ocd-host-agent.core :refer :all]
            [ocd-host-agent.docker-api :as docker]))

; (deftest test-list-containers
;   (let [containers (docker/list-containers "http://192.168.59.103:2375")]
;     (println (pr-str containers))))

; (deftest test-list-images
;   (let [images (docker/list-images "http://192.168.59.103:2375")]
;     (println (pr-str images))))

(deftest test-create-container
  (docker/run-container "tutum/hello-world"))