# Connector-core

Extension that registers default and core services as default implementations, http client, in memory stores and so on.

## Configuration settings

| Parameter name                           | Description                                                          | Mandatory | Default value |
|------------------------------------------|----------------------------------------------------------------------|-----------|---------------|
| `edc.hostname`                           | Connector hostname, which e.g. is used in referer urls               | false     | localhost     |
| `edc.http.enforce-https`                 | If true, enable HTTPS call enforcement.                              | false     | false         |
| `edc.core.retry.retries.max`             | Maximum retries for the retry policy before a failure is propagated. | false     | 5             |
| `edc.core.retry.backoff.min`             | Minimum number of milliseconds for exponential backoff.              | false     | 500           |
| `edc.core.retry.backoff.max`             | Maximum number of milliseconds for exponential backoff.              | false     | 10000         |
| `edc.core.retry.log.on.retry`            | Log Failsafe onRetry events.                                         | false     | false         |
| `edc.core.retry.log.on.retry.scheduled`  | Log Failsafe onRetryScheduled events.                                | false     | false         |
| `edc.core.retry.log.on.retries.exceeded` | Log Failsafe onRetriesExceeded events.                               | false     | false         |
| `edc.core.retry.log.on.failed.attempt`   | Log Failsafe onFailedAttempt events.                                 | false     | false         |
| `edc.core.retry.log.on.abort`            | Log Failsafe onAbort events.                                         | false     | false         |
