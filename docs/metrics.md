# EDC extensions for instrumentation with Micrometer

EDC provides extensions for instrumentation with the [Micrometer](https://micrometer.io/) metrics library to automatically collect metrics from the host system, JVM, and frameworks and libraries used in EDC (including OkHttp, Jetty, Jersey and ExecutorService).

See [sample 04.3](../samples/04.3-open-telemetry) for an example of an instrumented EDC consumer. 

## Micrometer Extension

This extension provides support for instrumentation for some core EDC components:
- JVM metrics
- [OkHttp](https://square.github.io/okhttp/) client metrics
- [ExecutorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html) metrics

## Jetty Micrometer Extension

This extension provides support for instrumentation for the [Jetty](https://www.eclipse.org/jetty/) web server, which is enabled when using the `JettyExtension`.

## Jersey Micrometer Extension

This extension provides support for instrumentation for the [Jersey](https://eclipse-ee4j.github.io/jersey/) framework, which is enabled when using the `JerseyExtension`.

## Instrumenting ExecutorServices

Instrumenting ExecutorServices requires using the `ExecutorInstrumentation` service to create a wrapper around the service to be instrumented:

```java
ExecutorInstrumentation executorInstrumentation = context.getService(ExecutorInstrumentation.class);

// instrument a ScheduledExecutorService
ScheduledExecutorService executor = executorInstrumentation.instrument(Executors.newScheduledThreadPool(10), "name");
```

Without any further configuration, a noop implementation of `ExecutorInstrumentation` is used. We recommend using the implementation provided in the Micrometer Extension that uses Micrometer's [ExecutorServiceMetrics](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/jvm/ExecutorServiceMetrics.java) to record ExecutorService metrics.

## Configuration

The following properties can use used to configure which metrics will be collected.

- `edc.metrics.enabled`: enables/disables metrics collection globally
- `edc.metrics.system.enabled`: enables/disables collection of system metrics (class loader, memory, garbage collection, processor and thread metrics)
- `edc.metrics.okhttp.enabled`: enables/disables collection of metrics for the OkHttp client
- `edc.metrics.executor.enabled`: enables/disables collection of metrics for the instrumented ExecutorServices
- `edc.metrics.jetty.enabled`: enables/disables collection of Jetty metrics
- `edc.metrics.jersey.enabled`: enables/disables collection of Jersey metrics

Default values are always "true", switch to "false" to disable the corresponding feature.
