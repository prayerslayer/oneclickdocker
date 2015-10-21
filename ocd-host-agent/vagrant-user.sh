#!/usr/bin/env bash

# install lein
java -version
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
sudo mv lein /usr/local/bin
sudo chmod a+x /usr/local/bin/lein
lein

export DOCKER_HOST="tcp://0.0.0.0:4243"
export DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -D"
export PATH=$PATH:/apps/redis-3.0.4/src/

redis-server