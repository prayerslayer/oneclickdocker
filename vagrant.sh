#!/usr/bin/env bash

mkdir /apps
apt-get update

# install docker
apt-get install -y curl
curl -sSL https://get.docker.com/ | sh
usermod -aG docker vagrant
# enable remote api
echo 'export DOCKER_HOST="tcp://0.0.0.0:4243"' >> ~/.profile
echo 'export DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -d -D"' >> ~/.profile
echo "" >> /etc/default/docker
echo 'DOCKER_HOST="tcp://0.0.0.0:4243"' >> /etc/default/docker
echo "" >> /etc/default/docker
echo 'DOCKER_OPTS="-s aufs -H tcp://0.0.0.0:4243 -D"' >> /etc/default/docker
# kernel extras for device driver
apt-get install "linux-image-extra-$(uname -r)"
service docker restart


# install redis
apt-get install -y wget tcl
wget http://download.redis.io/releases/redis-3.0.4.tar.gz
tar xzf redis-3.0.4.tar.gz
cd redis-3.0.4
make
make test
cd ..
mv redis-3.0.4 /apps
chmod a+x -R /apps/redis-3.0.4
echo 'export PATH=$PATH:/apps/redis-3.0.4/src/' >> ~/.profile

# install java
# TODO switch to java 8 as soon as it is in repo
apt-get install -y openjdk-7-jdk

# install nodejs
apt-get install -y nodejs npm
ln -s /usr/bin/nodejs /usr/bin/node

source ~/.profile

# i dunno?
if ! [ -L /var/www ]; then
  rm -rf /var/www
  ln -fs /vagrant /var/www
fi