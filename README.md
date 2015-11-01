# oneclickdocker

Hopefully this will be a set of tools that enable you to put a docker image online with one click.

## Subprojects

### ocd-host-agent

Used to host containers, write its status into redis.

### ocd-redis-proxy

Used to abstract away redis and gather information about containers from users.

### ocd-frontend

UI that will communicate with redis-proxy (actually i should call it ocd-api) to start and stop containers.
