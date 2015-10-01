#!/usr/bin/env bash

apt-get update

# install docker
apt-get install -y docker.io

# enable remote api
export DOCKER_HOST="tcp://0.0.0.0:4243"
echo "" >> /etc/default/docker.io
echo 'DOCKER_HOST="tcp://0.0.0.0:4243"' >> /etc/default/docker.io
service docker.io restart

# install java
# TODO switch to java 8 as soon as it is in repo
apt-get install -y openjdk-7-jdk 

# i dunno?
if ! [ -L /var/www ]; then
  rm -rf /var/www
  ln -fs /vagrant /var/www
fi