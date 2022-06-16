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

package org.eclipse.dataspaceconnector.sql.asset.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.translation.SqlConditionExpression;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static java.lang.String.format;
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
    public void accept(AssetEntry item) {
        Objects.requireNonNull(item);
        var asset = item.getAsset();
        var dataAddress = item.getDataAddress();

        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        try (var connection = getConnection()) {
            transactionContext.execute(() -> {
                try {
                    if (existsById(asset.getId(), connection)) {
                        throw new EdcPersistenceException(format("Cannot persist. Asset with ID '%s' already exists.", asset.getId()));
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
                                toPropertyValue(property.getValue()),
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


        var conditions = querySpec.getFilterExpression().stream().map(SqlConditionExpression::new).collect(Collectors.toList());
        var results = conditions.stream().map(SqlConditionExpression::isValidExpression).collect(Collectors.toList());

        if (results.stream().anyMatch(Result::failed)) {
            var message = results.stream().flatMap(r -> r.getFailureMessages().stream()).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(message);

        }
        var subSelects = conditions.stream().map(this::toSubSelect).collect(Collectors.toList());

        var template = "%s %s LIMIT %s OFFSET %s";
        var query = format(template, sqlAssetQueries.getSqlAssetListClause(), concatSubSelects(subSelects), querySpec.getLimit(), querySpec.getOffset());

        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var params = conditions.stream().flatMap(SqlConditionExpression::toStatementParameter).collect(Collectors.toList());

                List<String> ids = executeQuery(connection, this::mapAssetIds, query, params.toArray(Object[]::new));
                return ids.stream().map(this::findById);
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
            return single(dataAddressList);
        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(sqlAssetQueries.getCountVariableName());
    }

    AbstractMap.SimpleImmutableEntry<String, Object> mapPropertyResultSet(ResultSet resultSet) throws SQLException, ClassNotFoundException, JsonProcessingException {
        var name = resultSet.getString(sqlAssetQueries.getAssetPropertyColumnName());
        var value = resultSet.getString(sqlAssetQueries.getAssetPropertyColumnValue());
        var type = resultSet.getString(sqlAssetQueries.getAssetPropertyColumnType());


        return new AbstractMap.SimpleImmutableEntry<>(name, fromPropertyValue(value, type));
    }


    @Nullable
    private DataAddress single(List<DataAddress> dataAddressList) {
        if (dataAddressList.size() <= 0) {
            return null;
        } else if (dataAddressList.size() > 1) {
            throw new IllegalStateException("Expected result set size of 0 or 1 but got " + dataAddressList.size());
        } else {
            return dataAddressList.iterator().next();
        }
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

    /**
     * Concatenates all SELECT statements on all properties into one big statement, or returns "" if list is empty.
     */
    private String concatSubSelects(List<String> subSelects) {
        if (subSelects.isEmpty()) {
            return "";
        }
        return format(" WHERE %s", String.join(" AND ", subSelects));
    }

    /**
     * Converts a {@linkplain Criterion} into a dynamically assembled SELECT statement.
     */
    private String toSubSelect(SqlConditionExpression c) {
        return format("%s %s %s)", sqlAssetQueries.getQuerySubSelectClause(),
                c.getCriterion().getOperator(),
                c.toValuePlaceholder());
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
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private DataAddress mapDataAddress(ResultSet resultSet) throws SQLException, JsonProcessingException {
        return DataAddress.Builder.newInstance()
                .properties(objectMapper.readValue(resultSet.getString(sqlAssetQueries.getDataAddressColumnProperties()), HashMap.class))
                .build();
    }

    private String mapAssetIds(ResultSet resultSet) throws SQLException {
        return resultSet.getString(sqlAssetQueries.getAssetColumnId());
    }

    private String toPropertyValue(Object value) throws JsonProcessingException {
        return value instanceof String ? value.toString() : objectMapper.writeValueAsString(value);
    }
}
