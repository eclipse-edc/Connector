# SQL-based `TransferProcessStore` - technical proposal

**_Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may
work but might have unexpected side effects!_**

## Table schema DDL

please refer to [schema.sql](src/main/resources/transfer-process-schema.sql):

## Translating the `TransferProcessStore` interface into SQL statements

All SQL contants such as table names, column names, statement templates etc. are stored in an interface
called `TransferProcessStoreStatements`. Column names are declared directly in that interface, as they likely won't
change between different DB technologies, but all statements are then implemented in the `PostgreStatements` class.

## Create a flexible query API to accommodate `QuerySpec`

_For the first version, only the `limit` and `offset` arguments from the `QuerySpec` will be used._

For subsequent versions it is recommended to re-use the `Clause` interface and its implementors, that were originally
implemented for CosmosDB, and create an equivalent set of clauses for SQL. Thus, there would be a `Limit-`, `Offset-`
, `Order-` and `WhereClause` for SQL.

That way, dialect-dependent variants can be implemented should the need arise, because the actual SQL statement is
encoded in those clauses, offering a fluent Java API.

## Migrate from 0.5.1 to 0.6.0

The schema has changed, the columns contained in `edc_data_request` have been moved to `edc_transfer_process` with this
mapping:

- `datarequest_id` -> `correlation_id`
- `connector_address` -> `counter_party_address`
- `protocol` -> `protocol`
- `asset_id` -> `asset_id`
- `contract_id` -> `contract_id`
- `data_destination` -> `data_destination`

These columns need to be added to `edc_transfer_process`:

```sql
correlation_id             VARCHAR,
counter_party_address      VARCHAR,
protocol                   VARCHAR,
asset_id                   VARCHAR,
contract_id                VARCHAR,
data_destination           JSON,
```

then they should be filled with the corresponding value in the `edc_data_request` table, they can be joined by:
```edc_data_request.transfer_process_id = edc_transfer_process.transferprocess_id```

after that and after upgrading all the connector instance, the `edc_data_request` can be deleted.
