#!/bin/bash

# Declare a services list array
declare -a services=("auth-service" "harbor-service" "build-server-api" "build-server-builder" "version-set-service" "package-service")

# Print array values in  lines
echo "Building images for services"
for service in ${services[@]}; do
     echo Building $service
     #./gradlew :$service:build
     ./gradlew :$service:docker
done

