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

package org.eclipse.dataspaceconnector.sql.asset.index;

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

public class SqlAssetIndex implements AssetLoader, AssetIndex, DataAddressResolver {

    private final ObjectMapper objectMapper;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final SqlAssetQueries sqlAssetQueries;

    public SqlAssetIndex(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, SqlAssetQueries sqlAssetQueries) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.sqlAssetQueries = Objects.requireNonNull(sqlAssetQueries);
    }

    @Override
    public void accept(Asset asset, DataAddress dataAddress) {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        try (var connection = getConnection()) {
            transactionContext.execute(() -> {
                try {
                    if (existsById(asset.getId(), connection)) {
                        throw new EdcPersistenceException(String.format("Cannot persist. Asset with ID '%s' already exists.", asset.getId()));
                    }

                    executeQuery(connection, sqlAssetQueries.getSqlAssetInsertClause(),
                            asset.getId());
                    executeQuery(connection, sqlAssetQueries.getSqlDataAddressInsertClause(),
                            asset.getId(),
                            objectMapper.writeValueAsString(dataAddress.getProperties()));

                    for (var property : asset.getProperties().entrySet()) {
                        executeQuery(connection, sqlAssetQueries.getSqlPropertyInsertClause(),
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

        try (var connection = getConnection()) {
            var asset = findById(assetId);
            if (asset == null) {
                return null;
            }

            transactionContext.execute(() -> {
                executeQuery(connection, sqlAssetQueries.getSqlAssetDeleteByIdClause(), assetId);
                executeQuery(connection, sqlAssetQueries.getSqlDataAddressDeleteByIdClause(), assetId);
                executeQuery(connection, sqlAssetQueries.getSqlPropertyDeleteByIdClause(), assetId);
            });

            return asset;
        } catch (Exception e) {
            throw new EdcPersistenceException(e.getMessage(), e);
        }
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression);

        return getAssetStream(sqlAssetQueries.getSqlAssetListClause());
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);

        var query = String.format("%s LIMIT %s OFFSET %s",
                sqlAssetQueries.getSqlAssetListClause(),
                querySpec.getLimit(),
                querySpec.getOffset());

        return getAssetStream(query);
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        Objects.requireNonNull(assetId);

        try (var connection = getConnection()) {

            return transactionContext.execute(() -> {
                if (!existsById(assetId, connection)) {
                    return null;
                }
                var assetProperties = executeQuery(connection, this::mapPropertyResultSet, sqlAssetQueries.getSqlPropertyFindByIdClause(), assetId).stream().collect(Collectors.toMap(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        AbstractMap.SimpleImmutableEntry::getValue));
                return Asset.Builder.newInstance().id(assetId).properties(assetProperties).build();
            });

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
            var dataAddressList = transactionContext.execute(() -> executeQuery(connection, this::mapDataAddress, sqlAssetQueries.getSqlDataAddressFindByIdClause(), assetId));
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
            return transactionContext.execute(() -> {
                var assetIds = executeQuery(connection, this::mapAssetIds, query);
                if (assetIds.size() <= 0) {
                    return null;
                }

                var assets = new LinkedList<Asset>();
                for (var assetId : assetIds) {
                    var assetProperties = executeQuery(connection, this::mapPropertyResultSet, sqlAssetQueries.getSqlPropertyFindByIdClause(), assetId).stream().collect(Collectors.toMap(
                            AbstractMap.SimpleImmutableEntry::getKey,
                            AbstractMap.SimpleImmutableEntry::getValue));

                    assets.add(Asset.Builder.newInstance().id(assetId).properties(assetProperties).build());
                }
                return assets.stream();
            });

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    private boolean existsById(String assetId, Connection connection) {
        var assetCount = transactionContext.execute(() -> executeQuery(connection, this::mapRowCount, sqlAssetQueries.getSqlAssetCountByIdClause(), assetId).iterator().next());

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

    int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(sqlAssetQueries.getCountVariableName());
    }

    AbstractMap.SimpleImmutableEntry<String, Object> mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException, JsonProcessingException {
        return new AbstractMap.SimpleImmutableEntry<>(resultSet.getString(sqlAssetQueries.getAssetPropertyColumnName()),
                objectMapper.readValue(resultSet.getString(sqlAssetQueries.getAssetPropertyColumnValue()),
                        Class.forName(resultSet.getString(sqlAssetQueries.getAssetPropertyColumnType()))));
    }

    private DataAddress mapDataAddress(ResultSet resultSet) throws SQLException, JsonProcessingException {
        return DataAddress.Builder.newInstance()
                .properties(objectMapper.readValue(resultSet.getString(sqlAssetQueries.getDataAddressColumnProperties()), HashMap.class))
                .build();
    }

    private String mapAssetIds(ResultSet resultSet) throws SQLException {
        return resultSet.getString(sqlAssetQueries.getAssetColumnId());
    }
}
