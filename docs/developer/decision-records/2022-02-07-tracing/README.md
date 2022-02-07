# Tracing 

## Decision

Use [OpenTelemetry](https://opentelemetry.io/) to enable distributed tracing in EDC. 

[Context propagation](https://opentelemetry.io/docs/instrumentation/java/manual/#context-propagation) needs to be implemented accordingly so that traces are propagated across asynchronous workers. Business entities processed by async workers are used as carriers of tracing information, which is persisted together with the rest of the entity.

## Rationale

Distributed tracing is an essential observability pillar to correlate requests as they propagate through distributed cloud environments and services. EDC as a framework needs to support distributed tracing in any possible constellation where it might come to use. 

The OpenTelemetry Collector is a vendor-agnostic proxy that can receive, process, and export telemetry data to most common monitor backends, making it a very compelling option for EDC. 

## Span naming

Open telemetry spans are named according to the best practices mentioned in the [documentation](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#span):

```java
@WithSpan(value = "<ACTION>_<OBJECT_RECEIVING_ACTION>")

// e.g.
@WithSpan(value = "save_contract_negotiation")
```