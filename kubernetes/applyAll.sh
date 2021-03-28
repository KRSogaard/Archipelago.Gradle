#!/bin/bash
echo "Applying all files"
for entry in ./*.yaml
do
  kubectl apply -f $entry
done