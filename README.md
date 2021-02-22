# Dockerized local env

- Build all docker images for local repo:

```
./build_docker.sh
```

- Spin up local env:

```
docker-compose up -d
```

- Connect to common logs feed

```
docker-compose logs -f
```

- Connect to particular service logs feed (example package service):

```
docker-compose logs -f package-service
```

- Bring it down, and clear container volumes

```
docker-compose down -v
```

### HTTP Ports

Services exposed to localhost on following ports (specified in docker-compose.yml)

- auth-service: http://localhost:8087
- harbor-service: http://localhost:8093
- build-server-api: http://localhost:8095
- package-service: http://localhost:8090
- version-set-service: http://localhost:8091
