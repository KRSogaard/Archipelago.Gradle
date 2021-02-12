FROM adoptopenjdk/openjdk11:alpine
RUN apk --no-cache add curl
RUN addgroup -S spring && adduser -S spring -G spring

ARG APP_JAR_NAME=build-service-1.0.jar
ENV APP_JAR_NAME_ENV=$APP_JAR_NAME

# APP ENVS
ENV SPRING_PROFILE=local
ENV APP_PORT=8080
ENV APP_URL=http://localhost:8080

ADD . ./
WORKDIR /opt
RUN chown spring:spring /opt

USER spring:spring

ENTRYPOINT java \
    -Dspring.profiles.active=$SPRING_PROFILE \
    -jar $APP_JAR_NAME_ENV

# Enable after health-check endpoint implementation
#HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 CMD ["curl", "-f", "localhost:8080/actuator/health"]
