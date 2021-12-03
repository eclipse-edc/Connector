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

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;

@JsonTypeName("dataspaceconnector:federatedcatalognodedocument")
public class FederatedCacheNodeDocument extends CosmosDocument<FederatedCacheNode> {

    private String id;

    protected FederatedCacheNodeDocument() {
    }

    private FederatedCacheNodeDocument(FederatedCacheNode node, String partitionKey) {
        super(node, partitionKey);
        id = node.getName();
    }

    public static FederatedCacheNodeDocument from(FederatedCacheNode node, String partitionKey) {
        return new FederatedCacheNodeDocument(node, partitionKey);
    }

    public String getId() {
        return id;
    }
}
