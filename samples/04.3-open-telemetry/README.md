# Observe tracing of connectors with Jaeger.

## Run the sample

We will run a consumer and a provider, and observe HTTP tracing between these 2 services with [Jaeger](https://www.jaegertracing.io/).
[Jaeger](https://www.jaegertracing.io/) is an open source software for tracing transactions between services.

We will use docker compose to run the consumer, the provider and Jaeger.
Let's have a look to the [docker-compose.yaml file](docker-compose.yaml). We created a consumer and a provider service. 
Have a look to the entrypoints of the provider and the consumer. You can see that we provide the open-telemetry java agent.

Let's run the consumer, the provider and Jaeger:  

```bash
./gradlew samples:04.3-open-telemetry:consumer:build samples:04-file-transfer:provider:build
docker-compose -f samples/04.3-open-telemetry/docker-compose.yaml up
```

Once the consumer and provider are up, we can start a contract negotiation:

```bash
NEGOTIATION_ID=$(curl -X POST -H "Content-Type: application/json" -d @samples/04-file-transfer/contractoffer.json "http://localhost:9191/api/negotiation?connectorAddress=http://provider:8181/api/ids/multipart")
```

This causes an HTTP request sent from the consumer to the provider connector, followed by another message from the provider to the consumer connector.

You can access the jaeger UI on your browser on `http://localhost:16686`.
In the search tool, we can select the service `consumer` and click on  `Find traces`.
A trace represent an event, and is composed of spans.
If you click on one trace you can see more details about the spans composing the trace.