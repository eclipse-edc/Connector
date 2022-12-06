# Base

Extension that registers base services as default implementations, http client, in memory stores and so on.

## Configuration settings

| Parameter name           | Description                                            | Mandatory | Default value |
|--------------------------|--------------------------------------------------------|-----------|---------------|
| `edc.hostname`           | Connector hostname, which e.g. is used in referer urls | false     | localhost     |
| `edc.http.enforce-https` | If true, enable HTTPS call enforcement.                | false     | false         |
