# CosmosDB implementation of `ContractDefinitionStore`

This extension provides a persistent implementation of a the `ContractDefinitionStore` using a CosmosDB container.

The configuration values of this extension are listed below:

| Parameter name                                        | Description  | Mandatory | Default value |
|:------------------------------------------------------|:---------------| :---------| :-----------------|
| `edc.cosmos.partition-key`                            | This is the partition key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might produce incomplete results. |  false | dataspaceconnector |
| `edc.cosmos.query-metrics-enabled`                    | Enable metrics for query execution  | false | true |
| `edc.contractdefinitionstore.cosmos.account-name`     | Account name | true | null |
| `edc.contractdefinitionstore.cosmos.database-name`    | Name of the DB | true | null |
| `edc.contractdefinitionstore.cosmos.preferred-region` | Preferred region for Cosmos client instance | false | westeurope |
| `edc.contractdefinitionstore.cosmos.container-name`   | Name of container used to store Assets | true | null |
