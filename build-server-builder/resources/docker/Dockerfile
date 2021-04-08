FROM 075174350620.dkr.ecr.us-west-2.amazonaws.com/archipelago-base-image:latest
RUN apk update
RUN apk upgrade
RUN apk --no-cache add curl
RUN apk --no-cache add bash
# We need to remove this later
RUN apk --no-cache add rsync
RUN apk --no-cache add wget
RUN addgroup -S spring && adduser -S spring -G spring

RUN mkdir /opt/maui
RUN wget https://s3-us-west-2.amazonaws.com/alpha.archipelago.build/maui-builder-1.0.jar -P /opt/maui/; \
    echo '#!/bin/bash' >> /opt/maui/maui; \
    echo '/opt/java/openjdk/bin/java -jar /opt/maui/maui-builder-1.0.jar $@' >> /opt/maui/maui; \
    chmod a+r /opt/maui/maui-builder-1.0.jar; \
    chmod a+rx /opt/maui/maui;
ENV PATH=/opt/maui:$PATH

USER spring:spring

ARG APP_JAR_NAME=build-service-1.0.jar
ENV APP_JAR_NAME_ENV=$APP_JAR_NAME

ADD . ./
WORKDIR /opt
ENTRYPOINT java \
    -Dspring.profiles.active=$APPLICATION_STAGE \
    -jar $APP_JAR_NAME_ENV

# Enable after health-check endpoint implementation
#HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 CMD ["curl", "-f", "localhost:8080/actuator/health"]
