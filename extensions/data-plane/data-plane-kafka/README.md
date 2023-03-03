# Data Plane Kafka extension

## Background

### Scope

This extension provides support for consuming from and publishing to a Kafka broker.

### Limitations

> ⚠️ **This code is not intended for production-grade streaming pipelines!**

Even if it provides capabilities to stream events from an event source to a destination, this implementation drops
most of the inherent guarantees and benefits of a streaming pipeline, i.e. asynchronicity, ordering, resilience, delivery guarantee...
Depending on the protocol chosen to push the events to the consumer (e.g. http), it might also come with poor performances.

It also puts the pressure on the provider Data Plane which must operate the consuming threads (see `KafkaDataSource`) that
are constantly polling for events on the source topic. One thread being created for each transfer, it means that the number of threads
allocated by the provider Data Plane for fetching the events will linearly increase with the number of consumers, which might become
a limiting factor at some point.

Finally, the sink (see `HttpDataSink`) is publishing events asynchronously and in parallel, which means that there is no guarantee
on the final order in which events are delivered.

### Use Cases

Streaming events from the provider Kafka broker and pushing them to the consumer (potentially with a different protocol, such as http, websocket...).

## Technical Details

### `KafkaDataSource`

#### Data Address

This `DataSource` implementation is triggered when the type of the source address is set to `Kafka` (case-insensitive).
Parameters are listed below:

| Parameter    | Description                                                                            | Mandatory                                   | Default value                       |
|:-------------|:---------------------------------------------------------------------------------------|:--------------------------------------------|:------------------------------------|
| topic        | Broker topic from which events are consumed                                            | true                                        |                                     |
| kafka.*      | Kafka consumer properties                                                              | Only `kafka.bootstrap.servers` is mandatory |                                     |
| name         | Name of the transfer                                                                   | false                                       | `null`                              |
| maxDuration  | Duration of the stream, specified as ISO-8601 duration e.g. "PT10S" for 10 seconds     | false                                       | If not specified, stream never ends |
| pollDuration | Duration between two polls, specified as ISO-8601 duration e.g. "PT10S" for 10 seconds | false                                       | 1 second                            |

#### Consumer group

The consumer group is automatically determined from the request as followed:

```
<CONSUMER_GROUP>=<PROCESS_ID>:<REQUEST_ID>
```

### `KafkaDataSink`

#### Data address

This `DataSink` implementation is triggered when the type of the source address is set to `Kafka` (case-insensitive).
Parameters are listed below:

| Parameter | Description                                 | Mandatory                                   | Default value |
|:----------|:--------------------------------------------|:--------------------------------------------|:--------------|
| topic     | Broker topic from which events are consumed | true                                        |               |
| kafka.*   | Kafka producer properties                   | Only `kafka.bootstrap.servers` is mandatory |               |

#### Event publishing

Events are published in parallel and asynchronously (non-blocking). There is no retry-on-error nor guarantee on the order
in which events are published.