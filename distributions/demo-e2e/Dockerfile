FROM openjdk:11-jre-slim

WORKDIR /app
COPY ./build/libs/dagx-demo-e2e.jar /app
#COPY ./dagx-config-docker.properties /app/dagx-config.properties
#COPY ./cert.pfx /cert

EXPOSE 8181

ENTRYPOINT java \
    -Ddagx.vault.clientid=${CLIENTID} \
    -Ddagx.vault.tenantid=${TENANTID} \
    -Ddagx.vault.certificate=/cert/cert.pfx \
    -Ddagx.vault.name=${VAULTNAME} \
    -Ddagx.atlas.url=${ATLAS_URL} \
    -Ddagx.nifi.url=${NIFI_URL} \
    -Ddagx.nifi.flow.url=${NIFI_FLOW_URL} \
    -Ddagx.cosmos.account.name=${COSMOS_ACCOUNT} \
    -Ddagx.cosmos.database.name=${COSMOS_DB} \
    -Djava.security.edg=file:/dev/.urandom -jar dagx-demo-e2e.jar