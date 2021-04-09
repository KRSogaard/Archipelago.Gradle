#!/bin/bash

docker-compose down -v
./build_docker.sh
docker-compose up -d
