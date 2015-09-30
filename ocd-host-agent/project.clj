(defproject ocd-host-agent "0.1.0-SNAPSHOT"
  :description "fufufu"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ; component framework
                 [com.stuartsierra/component "0.3.0"]
                 ; task scheduler
                 [overtone/at-at "1.2.0"]
                 ; http/json stuff
                 [clj-http-lite "0.3.0"]
                 [cheshire "5.5.0"]
                 ; environment variables
                 [environ "1.0.1"]
                 ; web server
                 [ring "1.4.0"]
                 ; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [org.apache.logging.log4j/log4j-api "2.3"]
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.3"]
                 [org.apache.logging.log4j/log4j-jcl "2.3"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.3"]
                 [org.apache.logging.log4j/log4j-jul "2.3"]]
  :main ^:skip-aot ocd-host-agent.core
  :target-path "target"
  :profiles {:uberjar {:aot :all}})
