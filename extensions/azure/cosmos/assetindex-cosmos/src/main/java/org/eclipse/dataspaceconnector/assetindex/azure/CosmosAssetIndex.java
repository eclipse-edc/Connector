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

package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosAssetIndex implements AssetIndex, DataAddressResolver, AssetLoader {

    private final CosmosDbApi assetDb;
    private final String partitionKey;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final CosmosAssetQueryBuilder queryBuilder;

    /**
     * Creates a new instance of the CosmosDB-based for Asset storage.
     *
     * @param assetDb      Api to interact with Cosmos container.
     * @param partitionKey The CosmosDB partition key
     * @param typeManager  The {@link TypeManager} that's used for serialization and deserialization.
     * @param retryPolicy  Retry policy if query to CosmosDB fails.
     */
    public CosmosAssetIndex(CosmosDbApi assetDb, String partitionKey, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.assetDb = Objects.requireNonNull(assetDb);
        this.partitionKey = partitionKey;
        this.typeManager = Objects.requireNonNull(typeManager);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        queryBuilder = new CosmosAssetQueryBuilder();
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");

        SqlQuerySpec query = queryBuilder.from(expression);

        var response = with(retryPolicy).get(() -> assetDb.queryItems(query));
        return response.map(this::convertObject)
                .map(AssetDocument::getWrappedAsset);
    }

    @Override
    public Stream<Asset> queryAssets(List<Criterion> criteria) {

        SqlQuerySpec query = queryBuilder.from(criteria);
        var response = with(retryPolicy).get(() -> assetDb.queryItems(query));
        return response.map(this::convertObject)
                .map(AssetDocument::getWrappedAsset);
    }

    @Override
    public Asset findById(String assetId) {
        var result = queryByIdInternal(assetId);
        return result.map(AssetDocument::getWrappedAsset).orElse(null);
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        return queryByIdInternal(assetId).map(AssetDocument::getDataAddress).orElse(null);
    }

    @Override
    public void accept(Asset asset, DataAddress dataAddress) {
        var assetDocument = new AssetDocument(asset, partitionKey, dataAddress);
        assetDb.saveItem(assetDocument);
    }

    @Override
    public void accept(AssetEntry item) {
        accept(item.getAsset(), item.getDataAddress());
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
