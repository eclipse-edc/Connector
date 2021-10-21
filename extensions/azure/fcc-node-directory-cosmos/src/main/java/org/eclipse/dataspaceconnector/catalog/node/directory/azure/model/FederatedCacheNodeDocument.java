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

package org.eclipse.dataspaceconnector.catalog.node.directory.azure.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;

/**
 * This is a wrapper solely used to store {@link org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode} objects in an Azure CosmosDB.
 * Some features or requirements of CosmosDB don't fit into a {@link org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode}'s data model,
 * such as the "partition key", which is required by CosmosDB to achieve a better distribution of read/write load.
 *
 * @see org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode
 */
@JsonTypeName("dataspaceconnector:federatedcatalognodedocument")
public class FederatedCacheNodeDocument {

    @JsonProperty
    private final FederatedCacheNode wrappedInstance;

    @JsonProperty
    private final String partitionKey;

    @JsonCreator
    public FederatedCacheNodeDocument(@JsonProperty("wrappedInstance") FederatedCacheNode wrappedInstance,
                                      @JsonProperty("partitionKey") String partitionKey) {
        this.wrappedInstance = wrappedInstance;
        this.partitionKey = partitionKey;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public FederatedCacheNode getWrappedInstance() {
        return wrappedInstance;
    }
}
