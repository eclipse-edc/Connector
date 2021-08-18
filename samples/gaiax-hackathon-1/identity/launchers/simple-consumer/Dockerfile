FROM openjdk:11-jre-slim

WORKDIR /app
COPY ./build/libs/consumer.jar /app
#COPY ./dagx-config-docker.properties /app/dagx-config.properties
#COPY ./cert.pfx /cert

EXPOSE 8181

ENTRYPOINT java \
    -Dedc.vault.clientid=${CLIENTID} \
    -Dedc.vault.tenantid=${TENANTID} \
    -Dedc.vault.certificate=/cert/cert.pfx \
    -Dedc.vault.name=${VAULTNAME} \
    -Dedc.ids.connector.name=${CONNECTOR_NAME} \
    -Dedc.events.topic.name=${TOPIC_NAME} \
    -Dedc.events.topic.endpoint=${TOPIC_ENDPOINT} \
    -Djava.security.edg=file:/dev/.urandom -jar consumer.jar
