/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.asset.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_COLUMN_ID;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_PROPERTY_COLUMN_NAME;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_PROPERTY_COLUMN_TYPE;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_PROPERTY_COLUMN_VALUE;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_PROPERTY_TABLE;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.ASSET_TABLE;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.DATA_ADDRESS_COLUMN_PROPERTIES;
import static org.eclipse.dataspaceconnector.sql.asset.loader.SqlAssetTables.DATA_ADDRESS_TABLE;

public class SqlAssetLoader implements AssetLoader, AssetIndex, DataAddressResolver {
    private static final String SQL_ROW_COUNT_VARIABLE = "count";

    private static final String SQL_ASSET_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s) VALUES (?)",
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID,
            DATA_ADDRESS_COLUMN_PROPERTIES);
    private static final String SQL_PROPERTY_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID,
            ASSET_PROPERTY_COLUMN_NAME,
            ASSET_PROPERTY_COLUMN_VALUE,
            ASSET_PROPERTY_COLUMN_TYPE);

    private static final String SQL_ASSET_COUNT_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT COUNT(*) AS %s FROM %s WHERE %s = ?",
            SQL_ROW_COUNT_VARIABLE,
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_PROPERTY_FIND_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s WHERE %s = ?",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_FIND_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s WHERE %s = ?",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_LIST_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s",
            ASSET_TABLE);

    private static final String SQL_ASSET_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_PROPERTY_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID);


    private final ObjectMapper objectMapper;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;

    public SqlAssetLoader(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public void accept(Asset asset, DataAddress dataAddress) {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        try (var connection = getConnection()) {
            if (existsById(asset.getId(), connection)) {
                throw new EdcPersistenceException(String.format("Cannot persist. Asset with ID '%s' already exists.", asset.getId()));
            }

            transactionContext.execute(() -> {
                try {
                    executeQuery(connection, SQL_ASSET_INSERT_CLAUSE_TEMPLATE,
                            asset.getId());
                    executeQuery(connection, SQL_DATA_ADDRESS_INSERT_CLAUSE_TEMPLATE,
                            asset.getId(),
                            objectMapper.writeValueAsString(dataAddress.getProperties()));

                    for (var property : asset.getProperties().entrySet()) {
                        executeQuery(connection, SQL_PROPERTY_INSERT_CLAUSE_TEMPLATE,
                                asset.getId(),
                                property.getKey(),
                                objectMapper.writeValueAsString(property.getValue()),
                                property.getValue().getClass().getName());
                    }

                } catch (JsonProcessingException e) {
                    throw new EdcPersistenceException(e);
                }

            });

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            }
            throw new EdcPersistenceException(e.getMessage(), e);
        }
    }

    @Override
    public void accept(AssetEntry item) {
        Objects.requireNonNull(item);

        this.accept(item.getAsset(), item.getDataAddress());
    }

    @Override
    public Asset deleteById(String assetId) {
        Objects.requireNonNull(assetId);

        var asset = findById(assetId);
        if (asset == null) {
            return null;
        }

        try (var connection = getConnection()) {
            transactionContext.execute(() -> {
                executeQuery(connection, SQL_ASSET_PROPERTY_DELETE_BY_ID_CLAUSE_TEMPLATE, assetId);
                executeQuery(connection, SQL_DATA_ADDRESS_DELETE_BY_ID_CLAUSE_TEMPLATE, assetId);
                executeQuery(connection, SQL_ASSET_DELETE_BY_ID_CLAUSE_TEMPLATE, assetId);
            });
        } catch (Exception e) {
            throw new EdcPersistenceException(e.getMessage(), e);
        }

        return asset;
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression);

        return getAssetStream(SQL_ASSET_LIST_CLAUSE_TEMPLATE);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);

        var limit = Limit.Builder.newInstance().limit(querySpec.getLimit()).offset(querySpec.getOffset()).build();
        var query = SQL_ASSET_LIST_CLAUSE_TEMPLATE + " " + limit.getStatement();

        return getAssetStream(query);
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        Objects.requireNonNull(assetId);

        try (var connection = getConnection()) {
            if (!existsById(assetId, connection)) {
                return null;
            }

            var assetProperties = transactionContext.execute(() -> executeQuery(connection, this::mapPropertyResultSet, SQL_ASSET_PROPERTY_FIND_BY_ID_CLAUSE_TEMPLATE, assetId).stream().collect(Collectors.toMap(
                    AbstractMap.SimpleImmutableEntry::getKey,
                    AbstractMap.SimpleImmutableEntry::getValue)));

            return Asset.Builder.newInstance().id(assetId).properties(assetProperties).build();

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId);

        try (var connection = getConnection()) {
            var dataAddressList = transactionContext.execute(() -> executeQuery(connection, this::mapDataAddress, SQL_DATA_ADDRESS_FIND_BY_ID_CLAUSE_TEMPLATE, assetId));
            if (dataAddressList.size() <= 0) {
                return null;
            } else if (dataAddressList.size() > 1) {
                throw new IllegalStateException("Expected result set size of 0 or 1 but got " + dataAddressList.size());
            } else {
                return dataAddressList.iterator().next();
            }
        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }


    @Nullable
    private Stream<Asset> getAssetStream(String query) {
        try (var connection = getConnection()) {
            var assetIds = executeQuery(connection, this::mapAssetIds, query);
            if (assetIds.size() <= 0) {
                return null;
            }

            var assets = new LinkedList<Asset>();

            for (var assetId : assetIds) {
                var assetProperties = transactionContext.execute(() -> executeQuery(connection, this::mapPropertyResultSet, SQL_ASSET_PROPERTY_FIND_BY_ID_CLAUSE_TEMPLATE, assetId).stream().collect(Collectors.toMap(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        AbstractMap.SimpleImmutableEntry::getValue)));

                assets.add(Asset.Builder.newInstance().id(assetId).properties(assetProperties).build());
            }

            return assets.stream();

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    private boolean existsById(String definitionId, Connection connection) {
        var assetCount = transactionContext.execute(() -> executeQuery(connection, this::mapRowCount, SQL_ASSET_COUNT_BY_ID_CLAUSE_TEMPLATE, definitionId).iterator().next());

        if (assetCount <= 0) {
            return false;
        } else if (assetCount > 1) {
            throw new IllegalStateException("Expected result set size of 0 or 1 but got " + assetCount);
        } else {
            return true;
        }
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), String.format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(SQL_ROW_COUNT_VARIABLE);
    }

    private AbstractMap.SimpleImmutableEntry<String, Object> mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException, JsonProcessingException {
        return new AbstractMap.SimpleImmutableEntry<>(resultSet.getString(ASSET_PROPERTY_COLUMN_NAME),
                objectMapper.readValue(resultSet.getString(ASSET_PROPERTY_COLUMN_VALUE),
                        Class.forName(resultSet.getString(ASSET_PROPERTY_COLUMN_TYPE))));
    }

    private DataAddress mapDataAddress(ResultSet resultSet) throws SQLException, JsonProcessingException {
        return DataAddress.Builder.newInstance()
                .properties(objectMapper.readValue(resultSet.getString(DATA_ADDRESS_COLUMN_PROPERTIES), HashMap.class))
                .build();
    }

    private String mapAssetIds(ResultSet resultSet) throws SQLException {
        return resultSet.getString(ASSET_COLUMN_ID);
    }
}
