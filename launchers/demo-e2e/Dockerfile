FROM openjdk:11-jre-slim

WORKDIR /app
COPY ./build/libs/dataspaceconnector-demo-e2e.jar /app
#COPY ./dataspaceconnector-config-docker.properties /app/dataspaceconnector-config.properties
#COPY ./cert.pfx /cert

EXPOSE 8181

ENTRYPOINT java \
    -Dedc.vault.clientid=${CLIENTID} \
    -Dedc.vault.tenantid=${TENANTID} \
    -Dedc.vault.certificate=/cert/cert.pfx \
    -Dedc.vault.name=${VAULTNAME} \
    -Dedc.cosmos.account.name=${COSMOS_ACCOUNT} \
    -Dedc.cosmos.database.name=${COSMOS_DB} \
    -Dedc.events.topic.name=${TOPIC_NAME} \
    -Dedc.events.topic.endpoint=${TOPIC_ENDPOINT} \
    -Djava.security.edg=file:/dev/.urandom -jar dataspaceconnector-demo-e2e.jar
