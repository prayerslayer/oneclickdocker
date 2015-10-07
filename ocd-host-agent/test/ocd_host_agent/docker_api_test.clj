(ns ocd-host-agent.docker-api-test
  (:require [clojure.test :refer :all]
            [ocd-host-agent.core :refer :all]
            [ocd-host-agent.docker-api :as docker]))

; (deftest test-list-containers
;   (let [containers (docker/list-containers)]
;     (println (pr-str containers))))

; (deftest test-list-images
;   (let [images (docker/list-images)]
;     (println (pr-str images))))

; (deftest test-run-container
;   (docker/run-container "tutum/hello-world"))

; (deftest test-list-images
;   (println (pr-str (docker/list-images))))

; (deftest test-pull
;   (docker/pull "tutum/hello-world" nil))

(deftest test-create-container
  (println (:Id (docker/create-container "tutum/hello-world" nil))))

; (deftest test-downloaded?
;   (is (= (docker/downloaded? "tutum/hello-world") true))
;   (is (= (docker/downloaded? "tutum/hello-world" "latest") true))
;   (is (= (docker/downloaded? "ubuntu") false)))

; (deftest test-get-image
;   (is (= true (.startsWith (:Id (docker/get-image "tutum/hello-world" nil)) "c833a1892a15"))))