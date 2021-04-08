$services = @("auth-service","harbor-service","build-server-api","build-server-builder","version-set-service","package-service")

Write-Output  "Logging in to AWS"
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 075174350620.dkr.ecr.us-west-2.amazonaws.com


$buildId = Get-Date -Format yyyyMMdd-HHmmss
Write-Output "Using build id $buildId"

foreach ($service in $services)
{
  Write-Output "Building $service"
  ./gradlew :${service}:build
   ./gradlew :${service}:docker
   docker tag build.archipelago/${service}:latest 075174350620.dkr.ecr.us-west-2.amazonaws.com/${service}:${buildId}
   docker push 075174350620.dkr.ecr.us-west-2.amazonaws.com/${service}:${buildId}
   docker tag build.archipelago/${service}:latest 075174350620.dkr.ecr.us-west-2.amazonaws.com/${service}:latest
   docker push 075174350620.dkr.ecr.us-west-2.amazonaws.com/${service}:latest
}


Write-Output "Done building, the build id is: ${buildId}"