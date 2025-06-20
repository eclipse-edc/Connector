# Finalize phase

## Decision

We'll introduce a `finalize` phase in the `ServiceExtension` lifecycle.

## Rationale

The use of the registry pattern prevents direct dependency from the extension that injects a service and the one that
provides the registry entry.

For example, the `DataSourceRegistry` instance is provided by the `transaction` extensions, and injected to every extension
that provides SQL implementation for stores.
The actual `DataSource` it's provided by a third extension (in the `Connector` repository there's only `CommonsConnectionPoolServiceExtension`
that do that), which is not related in any way with the extension that provides the SQL store.

This means that at startup the extension initialization order could be:
- `DataSourceRegistry` provider extension
- `Sql` store implementation extension
- <other extensions depending on the `Sql` one>
- `DataSource` instantiation and registration extension

At shutdown, the order will be inverted, so at first the `DataSource` will be closed, then all the other extension will
follow.
This approach causes potential problems because it will cause failures in different extensions that use the `DataSource`

## Approach

In the same way we introduced the `prepare` phase after `initialization`, we will add a `finalize` phase that will be
executed after the `shutdown` one.

This method will be overridden by the extensions that provides such "basic" services that could be needed during `shutdown`
as sql data sources, websockets, mq connections and so on, while the `shutdown` method will continue to take care to shut
down services, threads and executor services.
