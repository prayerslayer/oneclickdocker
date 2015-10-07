#!/usr/bin/env bash

# install lein
java -version
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
sudo mv lein /usr/local/bin
sudo chmod a+x /usr/local/bin/lein
lein

# enable remote api
export DOCKER_HOST="tcp://0.0.0.0:4243"
echo "" >> /etc/default/docker.io
echo 'DOCKER_HOST="tcp://0.0.0.0:4243"' >> /etc/default/docker.io
export DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -d"
service docker.io restart