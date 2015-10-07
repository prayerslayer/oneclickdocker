#!/usr/bin/env bash

apt-get update

# install docker
apt-get install curl
curl -sSL https://get.docker.com/ | sh
usermod -aG docker vagrant
# enable remote api
export DOCKER_HOST="tcp://0.0.0.0:4243"
export DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -d -D"
echo "" >> /etc/default/docker
echo 'DOCKER_HOST="tcp://0.0.0.0:4243"' >> /etc/default/docker
echo "" >> /etc/default/docker
echo 'DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -D"' >> /etc/default/docker
# kernel extras for device driver
apt-get install "linux-image-extra-$(uname -r)"
service docker restart

# install java
# TODO switch to java 8 as soon as it is in repo
apt-get install -y openjdk-7-jdk 

# i dunno?
if ! [ -L /var/www ]; then
  rm -rf /var/www
  ln -fs /vagrant /var/www
fi