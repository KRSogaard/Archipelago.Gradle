#!/bin/bash

echo "Applying all files"
declare -a services=("auth-service-service.yaml" "build-server-api-service.yaml" "harbor-service-service.yaml" "package-service-service.yaml" "version-set-service-service.yaml")

for service in ${services[@]}; do
     echo "Applying $service"
     kubectl apply -f $service
done