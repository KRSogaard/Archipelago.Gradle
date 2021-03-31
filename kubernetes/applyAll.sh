#!/bin/bash

echo "Applying all files"
for entry in ./*deployment.yaml
do
  kubectl apply -f $entry
done

unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY