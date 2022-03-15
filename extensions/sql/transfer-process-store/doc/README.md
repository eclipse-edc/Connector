# SQL-based `TransferProcessStore` - technical proposal

**_Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may
work but might have unexpected side effects!_**

## Table schema DDL

please refer to [ddl_postgres.sql](./ddl_postgres.sql):

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