#!/bin/bash

# Declare a services list array
declare -a services=("auth-service" "harbor-service" "build-server-api" "build-server-builder" "version-set-service" "package-service")
# declare -a services=("build-server-builder")

echo "Logging in to AWS"
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 075174350620.dkr.ecr.us-west-2.amazonaws.com

# Print array values in  lines
echo "Building images for services"
for service in ${services[@]}; do
     echo Building $service
     ./gradlew :$service:build
     ./gradlew :$service:docker
     docker tag build.archipelago/$service:latest 075174350620.dkr.ecr.us-west-2.amazonaws.com/$service:20210402-1
     docker push 075174350620.dkr.ecr.us-west-2.amazonaws.com/$service:20210402-1
done

