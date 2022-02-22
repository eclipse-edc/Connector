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
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;

@JsonTypeName("dataspaceconnector:federatedcatalognodedocument")
public class FederatedCacheNodeDocument extends CosmosDocument<FederatedCacheNode> {

    @JsonCreator
    public FederatedCacheNodeDocument(@JsonProperty("wrappedInstance") FederatedCacheNode wrappedInstance,
                                      @JsonProperty("partitionKey") String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getName();
    }
}
