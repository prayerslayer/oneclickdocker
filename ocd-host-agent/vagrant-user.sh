#!/usr/bin/env bash

# install lein
java -version
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
sudo mv lein /usr/local/bin
sudo chmod a+x /usr/local/bin/lein
lein