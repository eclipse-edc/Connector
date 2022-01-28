# Visualize tracing of connectors with Open Telemetry and Jaeger

This sample shows how you can generate traces with Open Telemetry and collect and visualize them with Jaeger.

We will use the Open Telemetry java agent. It dynamically injects bytecode to capture telemetry from several popular [libraries and frameworks](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation).
In order to visualize and analyze the traces, we need to export them to a backend called an [exporter](https://opentelemetry.io/docs/instrumentation/js/exporters/).
We will use an exporter called [Jaeger](https://www.jaegertracing.io/).

## Prerequisites

Download the [opentelemetry-javaagent.jar](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) and place it in the project root folder.

## Run the sample

We will use docker-compose to run the consumer, the provider, and a Jaeger backend.
Let's have a look to the [docker-compose.yaml file](docker-compose.yaml). We created a consumer and a provider service.
Have a look at the entry points of the provider and the consumer. You can see that we provide the open-telemetry java agent.
Have a look at the environment variables. We provide the [Jaeger exporter env var](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#jaeger-exporter) needed by open telemetry.
Let's run the consumer, the provider, and Jaeger:

```bash
./gradlew samples:04.3-open-telemetry:consumer:build samples:04-file-transfer:provider:build
docker-compose -f samples/04.3-open-telemetry/docker-compose.yaml up
```

Once the consumer and provider are up, we can start a contract negotiation:

```bash
curl -X POST -H "Content-Type: application/json" -d @samples/04-file-transfer/contractoffer.json "http://localhost:9191/api/negotiation?connectorAddress=http://provider:8181/api/ids/multipart"
```

The contract negotiation causes an HTTP request sent from the consumer to the provider connector, followed by another message from the provider to the consumer connector.

You can access the jaeger UI on your browser on `http://localhost:16686`.
In the search tool, we can select the service `consumer` and click on `Find traces`.
A trace represents an event and is composed of spans.
If you click on one trace, you can see more details about the spans composing the trace.

Okhttp and jetty are part the [libraries and frameworks](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation) that open telemetry can capture telemetry from. We can observe spans related to okhttp and jetty as EDC uses both. Look at the `otel.library.name` tag of the different spans.
