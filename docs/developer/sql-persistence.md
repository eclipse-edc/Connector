# SQL persistence

Every store in the EDC, intended as persistence for state, comes out of the box with two implementations:
- in-memory
- sql (postgresql dialect)

By default, the `in-memory` stores are provided by the dependency injection, the `sql` implementations can be used by
simply registering the relative extensions (e.g. `asset-index-sql`, `contract-negotiation-store-sql`, ...).

## Configuration

### DataSources

For using `sql` extensions, a `DataSource` is needed, and it should be registered on the `DataSourceRegistry` service.

The `sql-pool-apache-commons` extension takes care to create and register pooled data sources starting from configuration.
It expects at least one data source called `default` that can be configured with `Vault` keys:
```
edc.datasource.default.url=...
edc.datasource.default.name=...
edc.datasource.default.password=...
```
(note: if no vault entries are found for such keys, they will be obtained from the configuration).

Other datasources can be defined using the same settings structure:
```
edc.datasource.<datasource-name>.url=...
edc.datasource.<datasource-name>.name=...
edc.datasource.<datasource-name>.password=...
```

`<datasource-name>` can be a string that then can be used by the stores configuration to use specific data sources.

### Using custom datasource in stores

Using a custom datasource in a store can be done by configuring the setting:
```
edc.sql.store.<store-context>.datasource=<datasource-name>
```

This way the `<store-context>` (that could be `asset`, `contractnegotiation`...) will use the `<datasource-name>` datasource.
