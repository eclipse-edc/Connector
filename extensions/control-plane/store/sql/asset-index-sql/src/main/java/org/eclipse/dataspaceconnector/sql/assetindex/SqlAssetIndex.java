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

package org.eclipse.dataspaceconnector.sql.assetindex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetEntry;
import org.eclipse.dataspaceconnector.sql.assetindex.schema.AssetStatements;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuerySingle;

public class SqlAssetIndex implements AssetIndex {

    private final ObjectMapper objectMapper;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final AssetStatements assetStatements;

    public SqlAssetIndex(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, AssetStatements assetStatements) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.assetStatements = Objects.requireNonNull(assetStatements);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression);

        var criteria = expression.getCriteria();
        var querySpec = QuerySpec.Builder.newInstance().filter(criteria)
                .offset(0)
                .limit(Integer.MAX_VALUE) // means effectively no limit
                .build();
        return queryAssets(querySpec);
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
    public void accept(AssetEntry item) {
        Objects.requireNonNull(item);
        var asset = item.getAsset();
        var dataAddress = item.getDataAddress();

        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        var assetId = asset.getId();
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                try {
                    if (existsById(assetId, connection)) {
                        deleteById(assetId);
                    }

                    executeQuery(connection, assetStatements.getInsertAssetTemplate(), assetId, asset.getCreatedAt());
                    var insertDataAddressTemplate = assetStatements.getInsertDataAddressTemplate();
                    executeQuery(connection, insertDataAddressTemplate, assetId, objectMapper.writeValueAsString(dataAddress.getProperties()));

                    for (var property : asset.getProperties().entrySet()) {
                        executeQuery(connection, assetStatements.getInsertPropertyTemplate(),
                                assetId,
                                property.getKey(),
                                toPropertyValue(property.getValue()),
                                property.getValue().getClass().getName());
                    }

                } catch (JsonProcessingException e) {
                    throw new EdcPersistenceException(e);
                }
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Asset deleteById(String assetId) {
        Objects.requireNonNull(assetId);

        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var asset = findById(assetId);
                if (asset == null) {
                    return null;
                }

                executeQuery(connection, assetStatements.getDeleteAssetByIdTemplate(), assetId);

                return asset;
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public long countAssets(QuerySpec querySpec) {
        try (var connection = getConnection()) {
            var statement = assetStatements.createQuery(querySpec);

            var queryAsString = statement.getQueryAsString().replace("SELECT * ", "SELECT COUNT (*) ");

            return executeQuerySingle(connection, true, r -> r.getLong(1), queryAsString, statement.getParameters());
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
        }
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

    private AbstractMap.SimpleImmutableEntry<String, Object> mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException, JsonProcessingException {
        var name = resultSet.getString(assetStatements.getAssetPropertyColumnName());
        var value = resultSet.getString(assetStatements.getAssetPropertyColumnValue());
        var type = resultSet.getString(assetStatements.getAssetPropertyColumnType());


        return new AbstractMap.SimpleImmutableEntry<>(name, fromPropertyValue(value, type));
    }

    /**
     * Deserializes a value into an object using the object mapper. Note: if type is {@code java.lang.String} simply
     * {@code value.toString()} is returned.
     */
    private Object fromPropertyValue(String value, String type) throws ClassNotFoundException, JsonProcessingException {
        var clazz = Class.forName(type);
        if (clazz == String.class) {
            return value;
        }
        return objectMapper.readValue(value, clazz);
    }

    private boolean existsById(String assetId, Connection connection) {
        var sql = assetStatements.getCountAssetByIdClause();
        try (var stream = executeQuery(connection, false, this::mapRowCount, sql, assetId)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private DataAddress mapDataAddress(ResultSet resultSet) throws SQLException, JsonProcessingException {
        return DataAddress.Builder.newInstance()
                .properties(objectMapper.readValue(resultSet.getString(assetStatements.getDataAddressColumnProperties()), new TypeReference<>() {
                }))
                .build();
    }

    private String mapAssetIds(ResultSet resultSet) throws SQLException {
        return resultSet.getString(assetStatements.getAssetIdColumn());
    }

    private String toPropertyValue(Object value) throws JsonProcessingException {
        return value instanceof String ? value.toString() : objectMapper.writeValueAsString(value);
    }
}
