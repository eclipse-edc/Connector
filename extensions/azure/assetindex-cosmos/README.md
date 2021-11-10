# CosmosDB implementation of AssetIndex

This extension provides a persistent implementation of an `AssetIndex` using a CosmosDB container.

The setting parameters of this extensions are listed below:

| Parameter name  | Description  | Mandatory | Default value |
| :-------------- |:---------------| :---------| :-----------------|
| `edc.assetindex.cosmos.account-name`  | Account name | true | null |
| `edc.assetindex.cosmos.database-name` | Name of the DB | true | null |
| `edc.cosmos.partition-key` | This is the partition key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might produce incomplete results. |  false | dataspaceconnector | 
| `edc.assetindex.cosmos.preferred-region` | Preferred region for Cosmos client instance | false | westeurope |
| `edc.assetindex.cosmos.container-name` | Name of container used to store Assets | true | null |
| `edc.cosmos.query-metrics-enabled` | Enable metrics for query execution  | false | true |