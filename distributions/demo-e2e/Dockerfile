FROM openjdk:11-jre-slim

WORKDIR /app
COPY ./build/libs/dagx-demo-e2e.jar /app
COPY ./dagx-config-docker.properties /app/dagx-config.properties
COPY ./cert.pfx /app

EXPOSE 8181

ENTRYPOINT ["java", "-Ddagx.fs.config=dagx-config.properties", "-Djava.security.edg=file:/dev/.urandom", "-jar", "dagx-demo-e2e.jar"]