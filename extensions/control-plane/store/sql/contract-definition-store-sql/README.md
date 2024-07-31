# SQL Contract Definition

Provides SQL persistence for contract definitions.

## Prerequisites

Please apply this [schema](src/main/resources/contract-definition-schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](docs/er.png)

## Create a flexible query API to accommodate `QuerySpec`

_For the first version, only the `limit` and `offset` arguments from the `QuerySpec` will be used._

For subsequent versions it is recommended to re-use the `Clause` interface and its implementors, that were originally
implemented for CosmosDB, and create an equivalent set of clauses for SQL. Thus, there would be a `Limit-`, `Offset-`
, `Order-` and `WhereClause` for SQL.

That way, dialect-dependent variants can be implemented should the need arise, because the actual SQL statement is
encoded in those clauses, offering a fluent Java API.
