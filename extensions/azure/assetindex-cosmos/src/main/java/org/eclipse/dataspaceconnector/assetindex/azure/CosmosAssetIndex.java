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
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Objects;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosAssetIndex implements AssetIndex {

    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final CosmosAssetQueryBuilder queryBuilder;

    /**
     * Creates a new instance of the CosmosDB-based for Asset storage.
     *
     * @param cosmosDbApi Api to interact with Cosmos container.
     * @param typeManager The {@link TypeManager} that's used for serialization and deserialization.
     * @param retryPolicy Retry policy if query to CosmosDB fails.
     */
    public CosmosAssetIndex(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = Objects.requireNonNull(cosmosDbApi);
        this.typeManager = Objects.requireNonNull(typeManager);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        queryBuilder = new CosmosAssetQueryBuilder();
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");

        SqlQuerySpec query = queryBuilder.from(expression);
        var response = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query.getQueryText()));
        return response.stream()
                .map(this::convertObject)
                .map(AssetDocument::getWrappedInstance);
    }

    @Override
    public Asset findById(String assetId) {
        // we need to read the AssetDocument as Object, because no custom JSON deserialization can be registered
        // with the CosmosDB SDK, so it would not know about subtypes, etc.
        var obj = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(assetId));
        return obj != null ? convertObject(obj).getWrappedInstance() : null;
    }

    private AssetDocument convertObject(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), AssetDocument.class);
    }
}
