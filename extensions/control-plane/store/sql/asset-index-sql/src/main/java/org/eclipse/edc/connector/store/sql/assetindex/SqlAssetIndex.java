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
 *       Microsoft Corporation - added full QuerySpec support
 *       ZF Friedrichshafen AG - added private property support
 *
 */

package org.eclipse.edc.connector.store.sql.assetindex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.store.sql.assetindex.schema.AssetStatements;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

public class SqlAssetIndex extends AbstractSqlStore implements AssetIndex {

    private final AssetStatements assetStatements;

    public SqlAssetIndex(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                         ObjectMapper objectMapper, AssetStatements assetStatements, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.assetStatements = Objects.requireNonNull(assetStatements);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);

        return transactionContext.execute(() -> {
            try {
                var statement = assetStatements.createQuery(querySpec);

                return queryExecutor.query(getConnection(), true, this::mapAssetIds, statement.getQueryAsString(), statement.getParameters())
                        .map(this::findById);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        Objects.requireNonNull(assetId);

        try (var connection = getConnection()) {

            return transactionContext.execute(() -> {
                if (!existsById(assetId, connection)) {
                    return null;
                }

                var selectAssetByIdSql = assetStatements.getSelectAssetByIdTemplate();
                var findPropertyByIdSql = assetStatements.getFindPropertyByIdTemplate();
                try (
                        var createdAtStream = queryExecutor.query(connection, false, this::mapCreatedAt, selectAssetByIdSql, assetId);
                        var allPropertiesStream = queryExecutor.query(connection, false, this::mapPropertyResultSet, findPropertyByIdSql, assetId)
                ) {
                    var createdAt = createdAtStream.findFirst().orElse(0L);
                    var groupedProperties = allPropertiesStream.collect(partitioningBy(SqlPropertyWrapper::isPrivate));
                    var assetProperties = groupedProperties.get(false).stream().collect(toMap(SqlPropertyWrapper::getPropertyKey, SqlPropertyWrapper::getPropertyValue));
                    var assetPrivateProperties = groupedProperties.get(true).stream().collect(toMap(SqlPropertyWrapper::getPropertyKey, SqlPropertyWrapper::getPropertyValue));
                    var dataAddress = resolveForAsset(assetId);
                    return Asset.Builder.newInstance()
                            .id(assetId)
                            .properties(assetProperties)
                            .privateProperties(assetPrivateProperties)
                            .createdAt(createdAt)
                            .dataAddress(dataAddress)
                            .build();
                }
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
    public StoreResult<Void> create(Asset asset) {
        Objects.requireNonNull(asset);
        var dataAddress = asset.getDataAddress();

        Objects.requireNonNull(dataAddress);

        var assetId = asset.getId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(assetId, connection)) {
                    var msg = format(ASSET_EXISTS_TEMPLATE, assetId);
                    return StoreResult.alreadyExists(msg);
                }

                if (asset.hasDuplicatePropertyKeys()) {
                    var msg = format(DUPLICATE_PROPERTY_KEYS_TEMPLATE);
                    return StoreResult.duplicateKeys(msg);
                }

                queryExecutor.execute(connection, assetStatements.getInsertAssetTemplate(), assetId, asset.getCreatedAt());
                var insertDataAddressTemplate = assetStatements.getInsertDataAddressTemplate();
                queryExecutor.execute(connection, insertDataAddressTemplate, assetId, toJson(dataAddress.getProperties()));

                insertProperties(asset, assetId, connection);

                return StoreResult.success();
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        Objects.requireNonNull(assetId);

        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var asset = findById(assetId);
                if (asset == null) {
                    return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId));
                }

                queryExecutor.execute(connection, assetStatements.getDeleteAssetByIdTemplate(), assetId);

                return StoreResult.success(asset);
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public long countAssets(List<Criterion> criteria) {
        try (var connection = getConnection()) {
            var statement = assetStatements.createQuery(criteria);

            var queryAsString = statement.getQueryAsString().replace("SELECT * ", "SELECT COUNT (*) ");

            return queryExecutor.single(connection, true, r -> r.getLong(1), queryAsString, statement.getParameters());
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
        }
    }

    @Override
    public StoreResult<Asset> updateAsset(Asset asset) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (asset.hasDuplicatePropertyKeys()) {
                    var msg = format(DUPLICATE_PROPERTY_KEYS_TEMPLATE);
                    return StoreResult.duplicateKeys(msg);
                }

                var assetId = asset.getId();
                if (existsById(assetId, connection)) {
                    queryExecutor.execute(connection, assetStatements.getDeletePropertyByIdTemplate(), assetId);
                    insertProperties(asset, assetId, connection);
                    return StoreResult.success(asset);
                }
                return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId));

            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<DataAddress> updateDataAddress(String assetId, DataAddress dataAddress) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(assetId, connection)) {
                    var updateTemplate = assetStatements.getUpdateDataAddressTemplate();
                    queryExecutor.execute(connection, updateTemplate, toJson(dataAddress.getProperties()), assetId);
                    return StoreResult.success(dataAddress);
                }
                return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId));

            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId);

        return transactionContext.execute(() -> {
            var sql = assetStatements.getFindDataAddressByIdTemplate();
            try {
                return queryExecutor.single(getConnection(), true, this::mapDataAddress, sql, assetId);
            } catch (Exception e) {
                if (e instanceof EdcPersistenceException) {
                    throw (EdcPersistenceException) e;
                } else {
                    throw new EdcPersistenceException(e.getMessage(), e);
                }
            }
        });
    }

    private long mapCreatedAt(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(assetStatements.getCreatedAtColumn());
    }

    private int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(assetStatements.getCountVariableName());
    }

    private SqlPropertyWrapper mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException {
        var name = resultSet.getString(assetStatements.getAssetPropertyNameColumn());
        var value = resultSet.getString(assetStatements.getAssetPropertyValueColumn());
        var type = resultSet.getString(assetStatements.getAssetPropertyTypeColumn());
        var isPrivate = resultSet.getBoolean(assetStatements.getAssetPropertyIsPrivateColumn());
        return new SqlPropertyWrapper(isPrivate, new AbstractMap.SimpleImmutableEntry<>(name, fromPropertyValue(value, type)));
    }

    /**
     * Deserializes a value into an object using the object mapper. Note: if type is {@code java.lang.String} simply
     * {@code value.toString()} is returned.
     */
    private Object fromPropertyValue(String value, String type) throws ClassNotFoundException {
        var clazz = Class.forName(type);
        if (clazz == String.class) {
            return value;
        }
        return fromJson(value, clazz);
    }

    private boolean existsById(String assetId, Connection connection) {
        var sql = assetStatements.getCountAssetByIdClause();
        try (var stream = queryExecutor.query(connection, false, this::mapRowCount, sql, assetId)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }

    private DataAddress mapDataAddress(ResultSet resultSet) throws SQLException {
        return DataAddress.Builder.newInstance()
                .properties(fromJson(resultSet.getString(assetStatements.getDataAddressPropertiesColumn()), new TypeReference<>() {
                }))
                .build();
    }

    private String mapAssetIds(ResultSet resultSet) throws SQLException {
        return resultSet.getString(assetStatements.getAssetIdColumn());
    }

    private void insertProperties(Asset asset, String assetId, Connection connection) {
        for (var property : asset.getProperties().entrySet()) {
            queryExecutor.execute(connection,
                    assetStatements.getInsertPropertyTemplate(),
                    assetId,
                    property.getKey(),
                    toJson(property.getValue()),
                    property.getValue().getClass().getName(),
                    false);
        }
        for (var privateProperty : asset.getPrivateProperties().entrySet()) {
            queryExecutor.execute(connection,
                    assetStatements.getInsertPropertyTemplate(),
                    assetId,
                    privateProperty.getKey(),
                    toJson(privateProperty.getValue()),
                    privateProperty.getValue().getClass().getName(),
                    true);
        }
    }

    private static class SqlPropertyWrapper {
        private final boolean isPrivate;
        private final AbstractMap.SimpleImmutableEntry<String, Object> property;

        protected SqlPropertyWrapper(boolean isPrivate, AbstractMap.SimpleImmutableEntry<String, Object> kvSimpleImmutableEntry) {
            this.isPrivate = isPrivate;
            this.property = kvSimpleImmutableEntry;
        }

        protected boolean isPrivate() {
            return isPrivate;
        }

        protected String getPropertyKey() {
            return property.getKey();
        }

        protected Object getPropertyValue() {
            return property.getValue();
        }
    }
}
