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

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosFederatedCacheNodeDirectory implements FederatedCacheNodeDirectory {

    private final CosmosContainer container;
    private final CosmosQueryRequestOptions tracingOptions;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final RetryPolicy<Object> retryPolicy;

    /**
     * Creates a new instance of the CosmosDB-based federated cache node store.
     *
     * @param container   The CosmosDB container.
     * @param typeManager The {@link TypeManager} that's used for serialization and deserialization
     */
    public CosmosFederatedCacheNodeDirectory(CosmosContainer container, String partitionKey, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {

        this.container = container;
        this.typeManager = typeManager;
        this.partitionKey = partitionKey;
        tracingOptions = new CosmosQueryRequestOptions();
        tracingOptions.setQueryMetricsEnabled(true);
        this.retryPolicy = retryPolicy;
    }

    @Override
    public List<FederatedCacheNode> getAll() {
        var query = "SELECT * FROM FederatedCatalogueNodeDocument";

        try {
            var response = with(retryPolicy).get(() -> container.queryItems(query, tracingOptions, Object.class));
            return response.stream()
                    .map(this::convertObject)
                    .map(FederatedCacheNodeDocument::getWrappedInstance)
                    .collect(Collectors.toList());
        } catch (CosmosException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public Stream<FederatedCacheNode> getAllAsync() {
        return getAll().stream();
    }

    @Override
    public void insert(FederatedCacheNode node) {
        Objects.requireNonNull(node.getName(), "FederatedCacheNode must have an name!");

        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        //todo: configure indexing
        var document = new FederatedCacheNodeDocument(node, partitionKey);
        try {
            var response = with(retryPolicy).get(() -> container.createItem(document, new PartitionKey(partitionKey), options));
            handleResponse(response);
        } catch (CosmosException cme) {
            throw new EdcException(cme);
        }
    }

    private void handleResponse(CosmosItemResponse<?> response) {
        int code = response.getStatusCode();
        if (code < 200 || code >= 300) {
            throw new EdcException("Error during CosmosDB interaction: " + code);
        }
    }

    private FederatedCacheNodeDocument convertObject(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), FederatedCacheNodeDocument.class);
    }
}
