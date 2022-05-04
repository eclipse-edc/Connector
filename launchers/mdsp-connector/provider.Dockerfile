FROM gradle:7.4.1-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project/
RUN gradle :samples:other:file-transfer-http-to-http:provider:shadowJar --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /home/gradle/project/samples/other/file-transfer-http-to-http/provider/build/libs/provider.jar /app
COPY --from=build /home/gradle/project/samples/other/file-transfer-http-to-http/provider/config.properties /app

EXPOSE 8181
EXPOSE 8182
EXPOSE 8282

ENTRYPOINT java \
    -Djava.security.edg=file:/dev/.urandom \
    -Dedc.ids.title="Eclipse Dataspace Connector" \
    -Dedc.ids.description="Eclipse Dataspace Connector with MindSphere HTTP extensions" \
    -Dedc.ids.maintainer="https://example.maintainer.com" \
    -Dedc.ids.curator="https://example.maintainer.com" \
    -Dedc.fs.config=/app/config.properties \
    -jar provider.jar
