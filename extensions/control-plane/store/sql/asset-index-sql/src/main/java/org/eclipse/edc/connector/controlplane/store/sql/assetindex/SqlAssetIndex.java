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

package org.eclipse.edc.connector.controlplane.store.sql.assetindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

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
                return queryExecutor.query(getConnection(), true, this::mapAsset, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        Objects.requireNonNull(assetId);

        try (var connection = getConnection()) {
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", assetId)).build();
            var statement = assetStatements.createQuery(querySpec);
            return queryExecutor.query(connection, true, this::mapAsset, statement.getQueryAsString(), statement.getParameters())
                    .findFirst().orElse(null);
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
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

                queryExecutor.execute(connection, assetStatements.getInsertAssetTemplate(),
                        assetId,
                        asset.getCreatedAt(),
                        toJson(asset.getProperties()),
                        toJson(asset.getPrivateProperties()),
                        toJson(asset.getDataAddress().getProperties()),
                        asset.getParticipantContextId()
                );

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
                var assetId = asset.getId();
                if (existsById(assetId, connection)) {

                    queryExecutor.execute(connection, assetStatements.getUpdateAssetTemplate(),
                            toJson(asset.getProperties()),
                            toJson(asset.getPrivateProperties()),
                            toJson(asset.getDataAddress().getProperties()),
                            assetId
                    );

                    return StoreResult.success(asset);
                }
                return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId));

            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        return Optional.ofNullable(findById(assetId)).map(Asset::getDataAddress).orElse(null);
    }

    private int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(assetStatements.getCountVariableName());
    }

    private boolean existsById(String assetId, Connection connection) {
        var sql = assetStatements.getCountAssetByIdClause();
        try (var stream = queryExecutor.query(connection, false, this::mapRowCount, sql, assetId)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }

    private Asset mapAsset(ResultSet resultSet) throws SQLException {
        return Asset.Builder.newInstance()
                .id(resultSet.getString(assetStatements.getAssetIdColumn()))
                .createdAt(resultSet.getLong(assetStatements.getCreatedAtColumn()))
                .properties(fromJson(resultSet.getString(assetStatements.getPropertiesColumn()), getTypeRef()))
                .privateProperties(fromJson(resultSet.getString(assetStatements.getPrivatePropertiesColumn()), getTypeRef()))
                .dataAddress(DataAddress.Builder.newInstance()
                        .properties(fromJson(resultSet.getString(assetStatements.getDataAddressColumn()), getTypeRef()))
                        .build())
                .participantContextId(resultSet.getString(assetStatements.getParticipantContextIdColumn()))
                .build();
    }

}
