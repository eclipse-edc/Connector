/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.SqlQuerySpec;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.connector.store.azure.cosmos.assetindex.model.AssetDocument;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;

public class CosmosAssetIndex implements AssetIndex {

    private final CosmosDbApi assetDb;
    private final String partitionKey;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final CosmosAssetQueryBuilder queryBuilder;
    private final Monitor monitor;

    /**
     * Creates a new instance of the CosmosDB-based for Asset storage.
     *
     * @param assetDb      Api to interact with Cosmos container.
     * @param partitionKey The CosmosDB partition key
     * @param typeManager  The {@link TypeManager} that's used for serialization and deserialization.
     * @param retryPolicy  Retry policy if query to CosmosDB fails.
     */
    public CosmosAssetIndex(CosmosDbApi assetDb, String partitionKey, TypeManager typeManager, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.assetDb = Objects.requireNonNull(assetDb);
        this.partitionKey = partitionKey;
        this.typeManager = Objects.requireNonNull(typeManager);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        this.monitor = monitor;
        queryBuilder = new CosmosAssetQueryBuilder();
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");

        if (expression.equals(AssetSelectorExpression.SELECT_ALL)) {
            return queryAssets(QuerySpec.none());
        }

        SqlQuerySpec query = queryBuilder.from(expression.getCriteria());

        var response = with(retryPolicy).get(() -> assetDb.queryItems(query));
        return response.map(this::convertObject)
                .map(AssetDocument::getWrappedAsset);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        var expr = querySpec.getFilterExpression();

        var sortField = querySpec.getSortField();
        var limit = querySpec.getLimit();
        var sortAsc = querySpec.getSortOrder() == SortOrder.ASC;

        var sqlQuery = queryBuilder.from(expr, sortField, sortAsc, limit, querySpec.getOffset());
        var response = with(retryPolicy).get(() -> assetDb.queryItems(sqlQuery));
        return response.map(this::convertObject)
                .map(AssetDocument::getWrappedAsset);
    }

    @Override
    public Asset findById(String assetId) {
        var result = queryByIdInternal(assetId);
        return result.map(AssetDocument::getWrappedAsset).orElse(null);
    }

    @Override
    public void accept(AssetEntry item) {
        var assetDocument = new AssetDocument(item.getAsset(), partitionKey, item.getDataAddress());
        assetDb.saveItem(assetDocument);
    }

    @Override
    public Asset deleteById(String assetId) {
        try {
            var deletedItem = assetDb.deleteItem(assetId);
            return convertObject(deletedItem).getWrappedAsset();
        } catch (NotFoundException notFoundException) {
            monitor.debug(() -> String.format("Asset with id %s not found", assetId));
            return null;
        }
    }

    @Override
    public long countAssets(List<Criterion> criteria) {
        var sqlQuery = queryBuilder.from(criteria);

        var stmt = sqlQuery.getQueryText().replace("SELECT * ", "SELECT COUNT(1) ");
        sqlQuery.setQueryText(stmt);
        return with(retryPolicy).get(() -> assetDb.queryItems(sqlQuery))
                .findFirst().map(this::extractCount).orElse(0L);
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        return queryByIdInternal(assetId).map(AssetDocument::getDataAddress).orElse(null);
    }

    private long extractCount(Object result) {
        if (result instanceof Map) {
            var map = (Map) result;
            return (Long) map.values().stream().findFirst().orElse(0L);
        }

        return 0L;
    }

    // we need to read the AssetDocument as Object, because no custom JSON deserialization can be registered
    // with the CosmosDB SDK, so it would not know about subtypes, etc.
    private AssetDocument convertObject(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), AssetDocument.class);
    }

    private Optional<AssetDocument> queryByIdInternal(String assetId) {
        var result = with(retryPolicy).get(() -> assetDb.queryItemById(assetId));
        return Optional.ofNullable(result).map(this::convertObject);
    }

}
