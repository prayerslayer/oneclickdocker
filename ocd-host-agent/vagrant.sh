#!/usr/bin/env bash

apt-get update

# install docker
apt-get install -y docker.io

# install java
# TODO switch to java 8 as soon as it is in repo
apt-get install -y openjdk-7-jdk 

# i dunno?
if ! [ -L /var/www ]; then
  rm -rf /var/www
  ln -fs /vagrant /var/www
fi