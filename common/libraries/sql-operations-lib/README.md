# SQL Operations Library

The SQL Operations Library provides helper classes for CRUD operations of EDC domain objects.

It executes the operations on the given `Connection`. The operations do not call `connection.commit()` or `
connection.rollback()`. These methods must be invoked by the caller.

## Samples

### Assets

#### Query

```java
class Demo {
    private Connection connection;

    public List<Asset> findAllAssets() throws SQLException {
        SqlAssetQuery query = new SqlAssetQuery(connection);
        return query.execute();
    }

    public List<Asset> queryAssets(Map<String, Object> filters) throws SQLException {
        SqlAssetQuery query = new SqlAssetQuery(connection);
        return query.execute(filters);
    }
}
```

#### Insert

```java
class Demo {
    private Connection connection;

    public void createAsset(Asset asset) throws SQLException {
        SqlAssetInsert delete = new SqlAssetInsert(connection);
        delete.execute(asset);
    }
}
```

#### Update

```java
class Demo {
    private Connection connection;

    public void updateAsset(Asset asset) throws SQLException {
        SqlAssetUpdate delete = new SqlAssetUpdate(connection);
        delete.execute(asset);
    }
}
```

#### Delete

```java
class Demo {
    private Connection connection;

    public void deleteAsset(String assetId) throws SQLException {
        SqlAssetDelete delete = new SqlAssetDelete(connection);
        delete.execute(assetId);
    }
}
```