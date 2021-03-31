FROM openjdk:11-jre-slim

ARG JAR_FILE=./runtime/build/libs/dagx-runtime.jar

RUN mkdir /app

COPY $JAR_FILE  /app/dagx-runtime.jar

RUN mkdir -p /etc/dagx

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "/app/dagx-runtime.jar"]

