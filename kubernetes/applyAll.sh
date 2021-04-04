#!/bin/bash

echo "Applying all files"
declare -a services=("auth-service-deployment.yaml" "build-server-api-deployment.yaml" "build-server-builder-deployment.yaml" "harbor-service-deployment.yaml" "package-service-deployment.yaml" "version-set-service-deployment.yaml")

for service in ${services[@]}; do
     echo "Applying $service"
     kubectl apply -f $service
done