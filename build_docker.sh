#!/bin/bash

# Declare a services list array
declare -a services=("auth-service" "harbor-service" "build-server-api" "version-set-service" "package-service")

# Print array values in  lines
echo "Building images for services"
for service in ${services[@]}; do
     echo Building $service
     cd ./$service
     ./gradlew docker
     cd ../
done

