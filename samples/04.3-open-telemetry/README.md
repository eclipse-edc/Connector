# Observe tracing of connectors with Jaeger.

## Run the sample

We will use docker compose to run the consumer, the provider and Jaeger.
Let's have a look to the [docker-compose.yaml file](docker-compose.yaml). We created a consumer and a provider service.
Have a look to the entrypoints. You can see that we provide the open-telemetry java agent. There is also a Jaeger service.

Let's run the consumer, the provider and Jaeger:  

```bash
./gradlew samples:04.3-open-telemetry:consumer:build samples:04-file-transfer:provider:build
docker-compose -f samples/04.3-open-telemetry/docker-compose.yaml up
```

Once the consumer and provider are up. Let's start a contract negotiation and get a contract agreement between the consumer and the provider. 

```bash
NEGOTIATION_ID=$(curl -X POST -H "Content-Type: application/json" -d @samples/04-file-transfer/contractoffer.json "http://localhost:9191/api/negotiation?connectorAddress=http://provider:8181/api/ids/multipart")
curl -X GET -H 'X-Api-Key: password' "http://localhost:9191/api/control/negotiation/${NEGOTIATION_ID}"
```

Access the jaeger UI on your browser on `http://localhost:16686`.
