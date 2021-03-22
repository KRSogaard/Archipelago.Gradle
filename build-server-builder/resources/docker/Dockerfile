FROM adoptopenjdk/openjdk11:alpine
RUN apk update
RUN apk upgrade
RUN apk --no-cache add curl
RUN apk --no-cache add bash
# We need to remove this later
RUN apk --no-cache add rsync
RUN addgroup -S spring && adduser -S spring -G spring
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
