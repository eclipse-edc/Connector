# SQL Pool Commons Library

The SQL Pool Commons Library implements the `ConnectionPool` interface for Apache Commons.

## Usage

### 1. Configuration

Configure the connection pool by creation a new instance of the `CommonsConnectionPoolConfig`.

### 2. Instantiation

Create a new instance of the `CommonsConnectionPool` using the `CommonsConnectionPoolConfig` and an instance of
a `ConnectionFactory`, from another EDC library.