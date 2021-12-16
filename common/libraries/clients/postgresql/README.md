# PostgreSQL Clients

This library provides a connection pooling client to perform SQL queries towards PostgreSQL databases. The
implementation prevents from SQL injection due to exclusive usage of `java.sql.PreparedStatement`s. It yields an
interface for transactional SQL queries and automatic transaction rollback while treating exceptions.

## Usage

```java
    ConnectionFactoryConfig connectionFactoryConfig = ConnectionFactoryConfig.Builder.newInstance()
        .uri(URI.create("jdbc://localhost:5432/mydb"))
        .userName("username")
        .password("password")
        .build();
    ConnectionFactory connectionFactory = new ConnectionFactoryImpl(connectionFactoryConfig);
    CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
    ConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);
    PostgresqlClient postgresqlClient = new PostgresqlClientImpl(connectionPool);

    List<Integer> i = postgresqlClient.execute((r) -> r.getInt(1), "SELECT 1;");

    postgresqlClient.close(); // closes all connections managed by the underlying pool
```