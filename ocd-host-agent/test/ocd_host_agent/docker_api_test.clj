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

(deftest test-run-container
  (docker/run-container "tutum/hello-world"))

; (deftest test-list-images
;   (println (pr-str (docker/list-images))))

; (deftest test-pull
;   (println (pr-str (docker/pull-image "tutum/hello-world"))))

; (deftest test-create-container
;   (println (pr-str (docker/create-container "tutum/hello-world"))))

; (deftest test-downloaded?
;   (is (= (docker/downloaded? "tutum/hello-world") true))
;   (is (= (docker/downloaded? "tutum/hello-world:latest") true))
;   (is (= (docker/downloaded? "ubuntu") false)))