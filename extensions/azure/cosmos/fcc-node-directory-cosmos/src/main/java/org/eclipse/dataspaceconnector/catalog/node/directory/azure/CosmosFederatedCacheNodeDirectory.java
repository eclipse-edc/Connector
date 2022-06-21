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

package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosFederatedCacheNodeDirectory implements FederatedCacheNodeDirectory {

    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final RetryPolicy<Object> retryPolicy;

    /**
     * Creates a new instance of the CosmosDB-based federated cache node store.
     *
     * @param cosmosDbApi Api to interact with CosmosDB container.
     * @param typeManager The {@link TypeManager} that's used for serialization and deserialization.
     */
    public CosmosFederatedCacheNodeDirectory(CosmosDbApi cosmosDbApi, String partitionKey, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.partitionKey = partitionKey;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public List<FederatedCacheNode> getAll() {
        var response = with(retryPolicy).get(() -> cosmosDbApi.queryAllItems(partitionKey));
        return response.stream()
                .map(databaseDocument -> typeManager.readValue(typeManager.writeValueAsString(databaseDocument), FederatedCacheNodeDocument.class))
                .map(FederatedCacheNodeDocument::getWrappedInstance)
                .collect(Collectors.toList());
    }

    @Override
    public void insert(FederatedCacheNode node) {
        Objects.requireNonNull(node.getName(), "FederatedCacheNode must have a name!");

        var document = new FederatedCacheNodeDocument(node, partitionKey);
        with(retryPolicy).run(() -> cosmosDbApi.saveItem(document));
    }
}
