# SQL Pool Apache Commons Pool

This extension registers named `javax.sql.DataSource`s to
the `org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry`
capable of pooling `java.sql.Connection`s. The pooling mechanism is backed by
the [Apache Commons Pool library](https://commons.apache.org/proper/commons-pool/).

## Configuration

| Key | Description | Mandatory | 
|:---|:---|---|
| edc.datasource.<datasource_name>.url | JDBC driver url | X |
| edc.datasource.<datasource_name>.pool.maxIdleConnections | The maximum amount of idling connections maintained by the pool | | 
| edc.datasource.<datasource_name>.pool.maxTotalConnections | The maximum amount of total connections maintained by the pool | |
| edc.datasource.<datasource_name>.pool.minIdleConnections | The minimum amount of idling connections maintained by the pool | |
| edc.datasource.<datasource_name>.pool.testConnectionOnBorrow | Flag to define whether connections will be validated when a connection has been obtained from the pool | |
| edc.datasource.<datasource_name>.pool.testConnectionOnCreate | Flag to define whether connections will be validated when a connection has been established | |
| edc.datasource.<datasource_name>.pool.testConnectionOnReturn | Flag to define whether connections will be validated when a connection has been returned to the pool | |
| edc.datasource.<datasource_name>.pool.testConnectionWhileIdle | Flag to define whether idling connections will be validated | |
| edc.datasource.<datasource_name>.pool.testQuery | Test query to validate a connection maintained by the pool | |
| edc.datasource.<datasource_name>.<jdbc_properties> | JDBC driver specific configuration properties | |
