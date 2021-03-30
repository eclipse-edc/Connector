FROM gradle:jdk11 AS build

#ARG SECURITY

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

#RUN if [ -z "$SECURITY" ] ; \
#    then \
#        gradle -Dsecurity.type=fs runtime:clean runtime:shadowJar --no-daemon ; \
#    else \
#        gradle runtime:clean runtime:shadowJar --no-daemon ; \
#    fi

RUN gradle -Dsecurity.type=fs runtime:clean runtime:shadowJar --no-daemon


FROM openjdk:11-jre-slim

# variables will be passed during "docker run"

ENV DAGX_VAULT=dagx-vault.properties
ENV DAGX_KEYSTORE=dagx-keystore.jks
ENV DAGX_KEYSTORE_PASSWORD=yourpwdhere

RUN mkdir /app

COPY --from=build /home/gradle/src/runtime/build/libs/dagx-runtime.jar /app/dagx-runtime.jar

RUN mkdir -p /etc/dagx

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "/app/dagx-runtime.jar"]

