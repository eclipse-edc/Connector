FROM openjdk:17-slim

WORKDIR /app
COPY ./build/libs/reg-svc.jar /app

EXPOSE 8181

ENTRYPOINT java \
    -Dedc.vault.clientid=${CLIENTID} \
    -Dedc.vault.tenantid=${TENANTID} \
    -Dedc.vault.certificate=/cert/cert.pfx \
    -Dedc.vault.name=${VAULTNAME} \
    -Dedc.events.topic.name=${TOPIC_NAME} \
    -Ddataspaceconnector.connector.name=${CONNECTOR_NAME} \
    -Dedc.events.topic.endpoint=${TOPIC_ENDPOINT} \
    -Dedc.ion.crawler.ion.url=${ION_URL} \
    -Dedc.ion.crawler.randomize=false \
    -Djava.security.edg=file:/dev/.urandom -jar reg-svc.jar
