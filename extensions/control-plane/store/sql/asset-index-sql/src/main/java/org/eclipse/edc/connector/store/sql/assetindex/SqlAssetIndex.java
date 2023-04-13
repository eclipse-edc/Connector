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
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
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
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuerySingle;

public class SqlAssetIndex extends AbstractSqlStore implements AssetIndex {

    private final AssetStatements assetStatements;

    public SqlAssetIndex(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, AssetStatements assetStatements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.assetStatements = Objects.requireNonNull(assetStatements);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);

        return transactionContext.execute(() -> {
            try {
                var statement = assetStatements.createQuery(querySpec);

                return executeQuery(getConnection(), true, this::mapAssetIds, statement.getQueryAsString(), statement.getParameters())
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
                        var createdAtStream = executeQuery(connection, false, this::mapCreatedAt, selectAssetByIdSql, assetId);
                        var propertiesStream = executeQuery(connection, false, this::mapPropertyResultSet, findPropertyByIdSql, assetId)
                ) {
                    var createdAt = createdAtStream.findFirst().orElse(0L);
                    var assetProperties = propertiesStream
                            .collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));

                    return Asset.Builder.newInstance()
                            .id(assetId)
                            .properties(assetProperties)
                            .createdAt(createdAt)
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
    public StoreResult<Void> accept(AssetEntry item) {
        Objects.requireNonNull(item);
        var asset = item.getAsset();
        var dataAddress = item.getDataAddress();

        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        var assetId = asset.getId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(assetId, connection)) {
                    var msg = format(ASSET_EXISTS_TEMPLATE, assetId);
                    return StoreResult.alreadyExists(msg);
                }

                executeQuery(connection, assetStatements.getInsertAssetTemplate(), assetId, asset.getCreatedAt());
                var insertDataAddressTemplate = assetStatements.getInsertDataAddressTemplate();
                executeQuery(connection, insertDataAddressTemplate, assetId, toJson(dataAddress.getProperties()));

                for (var property : asset.getProperties().entrySet()) {
                    executeQuery(connection, assetStatements.getInsertPropertyTemplate(),
                            assetId,
                            property.getKey(),
                            toJson(property.getValue()),
                            property.getValue().getClass().getName());
                }
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

                executeQuery(connection, assetStatements.getDeleteAssetByIdTemplate(), assetId);

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

            return executeQuerySingle(connection, true, r -> r.getLong(1), queryAsString, statement.getParameters());
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
        }
    }

    @Override
    public StoreResult<Asset> updateAsset(Asset asset) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var assetId = asset.getId();
                if (existsById(assetId, connection)) {
                    executeQuery(connection, assetStatements.getDeletePropertyByIdTemplate(), assetId);
                    for (var property : asset.getProperties().entrySet()) {
                        executeQuery(connection, assetStatements.getInsertPropertyTemplate(),
                                assetId,
                                property.getKey(),
                                toJson(property.getValue()),
                                property.getValue().getClass().getName());
                    }
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
                    executeQuery(connection, updateTemplate, toJson(dataAddress.getProperties()), assetId);
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
                return executeQuerySingle(getConnection(), true, this::mapDataAddress, sql, assetId);
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

    private AbstractMap.SimpleImmutableEntry<String, Object> mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException {
        var name = resultSet.getString(assetStatements.getAssetPropertyColumnName());
        var value = resultSet.getString(assetStatements.getAssetPropertyColumnValue());
        var type = resultSet.getString(assetStatements.getAssetPropertyColumnType());


        return new AbstractMap.SimpleImmutableEntry<>(name, fromPropertyValue(value, type));
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
        try (var stream = executeQuery(connection, false, this::mapRowCount, sql, assetId)) {
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


}
