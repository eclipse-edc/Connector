# System tests

## Tracing integration tests

The [TracingIntegrationTest](./tests/src/test/java/org/eclipse/dataspaceconnector/system/tests/local/FileTransferIntegrationTest.java) makes sure that tracing works correctly when triggering a file transfer.
This test triggers a file transfer with the [opentelemetry java agent attached](https://github.com/open-telemetry/opentelemetry-java-instrumentation). The default trace exporter configured in the agent is the OTLP exporter based on gRPC protocol. Therefore, a test OtlpGrpcServer is started to collect the traces.
To be able to run the `TracingIntegrationTest` locally, you need to place the [opentelemetry java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.0/opentelemetry-javaagent.jar) in the project root folder.
Then you can run the test:
```bash
./gradlew -p system-tests/tests test -DincludeTags="OpenTelemetryIntegrationTest"
```