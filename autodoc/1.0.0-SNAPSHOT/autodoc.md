Module `transfer-tasks-subscriber-nats`
---------------------------------------
**Artifact:** org.eclipse.edc:transfer-tasks-subscriber-nats:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.virtual.controlplane.transfer.subscriber.nats.NatsTransferProcessTaskSubscriberExtension`
**Name:** "NatsTransferProcessTaskSubscriberExtension"

### Configuration

| Key                                          | Required | Type     | Default                 | Pattern | Min | Max | Description                                                                                                                                                                                                                                                                       |
| -------------------------------------------- | -------- | -------- | ----------------------- | ------- | --- | --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.nats.tp.subscriber.url`                 | `*`      | `string` | `nats://localhost:4222` |         |     |     | The URL of the NATS server to connect to for transfer process events.                                                                                                                                                                                                             |
| `edc.nats.tp.subscriber.name`                | `*`      | `string` | `tp-subscriber`         |         |     |     | The name of the consumer for transfer process events                                                                                                                                                                                                                              |
| `edc.nats.tp.subscriber.autocreate`          | `*`      | `string` | `false`                 |         |     |     | Convenience flag: when true, it will automatically create both the stream and the consumer if not present                                                                                                                                                                         |
| `edc.nats.tp.subscriber.stream.autocreate`   | `*`      | `string` | `false`                 |         |     |     | When true, it will automatically create the stream if not present                                                                                                                                                                                                                 |
| `edc.nats.tp.subscriber.consumer.autocreate` | `*`      | `string` | `false`                 |         |     |     | When true, it will automatically create the consumer if not present                                                                                                                                                                                                               |
| `edc.nats.tp.subscriber.stream`              | `*`      | `string` | `tp-stream`             |         |     |     | The stream name where to attach the consumer                                                                                                                                                                                                                                      |
| `edc.nats.tp.subscriber.subject`             | `*`      | `string` | `transfers.>`           |         |     |     | The subject of the consumer for transfer process events                                                                                                                                                                                                                           |
| `edc.nats.tp.subscriber.batch-size`          | `*`      | `string` | `100`                   |         |     |     | The size of the batch when fetching messages                                                                                                                                                                                                                                      |
| `edc.nats.tp.subscriber.max-wait`            | `*`      | `string` | `100`                   |         |     |     | The max waiting time for messages (ms)                                                                                                                                                                                                                                            |
| `edc.nats.tp.subscriber.max-retries`         | `*`      | `string` | `3`                     |         |     |     | Max number of message deliveries while the referenced task is not yet visible in the store (e.g. published before its transaction committed), after which the message is dropped. It does not cap processor/business retries, which are bounded by edc.transfer.send.retry.limit. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.controlplane.tasks.TaskService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `io.nats.client.Options` (optional)

Module `transfer-tasks-subscriber-nats`
---------------------------------------
**Artifact:** org.eclipse.edc:transfer-tasks-subscriber-nats:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.virtual.controlplane.transfer.subscriber.nats.NatsTransferProcessTaskSubscriberExtension`
**Name:** "NatsTransferProcessTaskSubscriberExtension"

### Configuration

| Key                                          | Required | Type     | Default                 | Pattern | Min | Max | Description                                                                                                                                                                                                                                                                       |
| -------------------------------------------- | -------- | -------- | ----------------------- | ------- | --- | --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.nats.tp.subscriber.url`                 | `*`      | `string` | `nats://localhost:4222` |         |     |     | The URL of the NATS server to connect to for transfer process events.                                                                                                                                                                                                             |
| `edc.nats.tp.subscriber.name`                | `*`      | `string` | `tp-subscriber`         |         |     |     | The name of the consumer for transfer process events                                                                                                                                                                                                                              |
| `edc.nats.tp.subscriber.autocreate`          | `*`      | `string` | `false`                 |         |     |     | Convenience flag: when true, it will automatically create both the stream and the consumer if not present                                                                                                                                                                         |
| `edc.nats.tp.subscriber.stream.autocreate`   | `*`      | `string` | `false`                 |         |     |     | When true, it will automatically create the stream if not present                                                                                                                                                                                                                 |
| `edc.nats.tp.subscriber.consumer.autocreate` | `*`      | `string` | `false`                 |         |     |     | When true, it will automatically create the consumer if not present                                                                                                                                                                                                               |
| `edc.nats.tp.subscriber.stream`              | `*`      | `string` | `tp-stream`             |         |     |     | The stream name where to attach the consumer                                                                                                                                                                                                                                      |
| `edc.nats.tp.subscriber.subject`             | `*`      | `string` | `transfers.>`           |         |     |     | The subject of the consumer for transfer process events                                                                                                                                                                                                                           |
| `edc.nats.tp.subscriber.batch-size`          | `*`      | `string` | `100`                   |         |     |     | The size of the batch when fetching messages                                                                                                                                                                                                                                      |
| `edc.nats.tp.subscriber.max-wait`            | `*`      | `string` | `100`                   |         |     |     | The max waiting time for messages (ms)                                                                                                                                                                                                                                            |
| `edc.nats.tp.subscriber.max-retries`         | `*`      | `string` | `3`                     |         |     |     | Max number of message deliveries while the referenced task is not yet visible in the store (e.g. published before its transaction committed), after which the message is dropped. It does not cap processor/business retries, which are bounded by edc.transfer.send.retry.limit. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.controlplane.tasks.TaskService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `io.nats.client.Options` (optional)

