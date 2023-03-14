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

import com.azure.cosmos.implementation.ConflictException;
import com.azure.cosmos.implementation.NotFoundException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.connector.store.azure.cosmos.assetindex.model.AssetDocument;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
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
import static java.lang.String.format;

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
    public StoreResult<Void> create(AssetEntry item) {
        var assetDocument = new AssetDocument(item.getAsset(), partitionKey, item.getDataAddress());
        try {
            assetDb.createItem(assetDocument);
        } catch (ConflictException ex) {
            var id = item.getAsset().getId();
            var msg = format(ASSET_EXISTS_TEMPLATE, id);
            monitor.debug(() -> msg);
            return StoreResult.alreadyExists(msg);
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        try {
            var deletedItem = assetDb.deleteItem(assetId);
            // todo: the CosmosDbApi should not throw an exception when the item isn't found
            return StoreResult.success(convertObject(deletedItem).getWrappedAsset());
        } catch (NotFoundException nfe) {
            var msg = format(ASSET_NOT_FOUND_TEMPLATE, assetId);
            monitor.debug(() -> msg);
            return StoreResult.notFound(msg);
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
    public StoreResult<Asset> updateAsset(Asset asset) {
        // cannot simply invoke assetDb.updateItem, because for that we'd need the DataAddress, which we don't have here
        var assetId = asset.getId();
        var result = queryByIdInternal(assetId);

        return result.map(assetDocument -> {
            var updated = new AssetDocument(asset, assetDocument.getPartitionKey(), assetDocument.getDataAddress());

            // the following statement could theoretically still raise a NotFoundException, but at that point we'll let it bubble up the stack
            assetDb.updateItem(updated);
            return StoreResult.success(asset);
        }).orElse(StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId)));
    }

    @Override
    public StoreResult<DataAddress> updateDataAddress(String assetId, DataAddress dataAddress) {
        // cannot simply invoke assetDb.saveItem, because for that we'd need the Asset, which we don't have here
        var result = queryByIdInternal(assetId);

        return result.map(assetDocument -> {
            var updated = new AssetDocument(assetDocument.getWrappedAsset(), assetDocument.getPartitionKey(), dataAddress);

            // the following statement could theoretically still raise a NotFoundException, but at that point we'll let it bubble up the stack
            assetDb.updateItem(updated);
            return StoreResult.success(dataAddress);
        }).orElse(StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId)));
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
