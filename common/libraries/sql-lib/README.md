# SQL Library

## Components

### SQL Query Executor

The SQL Library comes with an `SqlQueryExecutor`, that may be used to execute queries on a
database `java.sql.Connection`.

### Connection Pool

The SQL library defines an `ConnectionPool` interface. The connection pool creates and manages multiple instances of
a `java.sql.Connection`. This interface may be implemented by other EDC libraries and is one way to retrieve
a `java.sql.Connection`.

### Connection Factory

The SQL library defines an `ConnectionFactory` interface. The connection factory creates a `java.sql.Connection` for a
certain kind of database (e.g. PostgreSQL). This interface and may be implemented by other EDC libraries and is one way
to retrieve a `java.sql.Connection`.

## Usage

### 1. Get a `java.sqlConnection`

The connection may be created by any arbitrary java library or by another EDC library, that implements
the `ConnectionPool` or `ConnectionFactory`.

### 2. Call the `SqlQueryExecutor`

The `SqlQueryExecutor` may be called with a `java.sql.Connection`, the query itself and optional query parameters.

Please note that the `SqlQueryExecutor` does not manage the `java.sql.Connection` itself. This must be done by the
caller.
