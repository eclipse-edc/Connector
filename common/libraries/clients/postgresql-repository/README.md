# PostgreSQL-based Repository

This library provides an PostgreSQL `Repository`.

```java
public interface Repository {

    List<Asset> query(List<Criterion> criteria) throws SQLException;

    void create(Asset asset) throws SQLException;

    void update(Asset asset) throws SQLException;

    void delete(Asset asset) throws SQLException;
}
```

## Usage

### AssetRepositoryImpl

```java
    PostgresqlClient postgresqlClient = /* see README.md of the PostgresqlClient */
    Repository repository = new RepositoryImpl(postgresqlClient);
```
