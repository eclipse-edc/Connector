/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class TestUtil {

    public static final String TEST_PROTOCOL = "test-protocol";

    public static WorkItem createWorkItem() {
        return new WorkItem("test-url", "test-protocol");
    }

    @NotNull
    public static ContractOffer createOffer(String id) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .asset(Asset.Builder.newInstance().id(id).build())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    public static Catalog createCatalog() {
        return Catalog.Builder.newInstance()
                .id("test-catalog")
                .contractOffers(List.of(createOffer("test-offer")))
                .build();
    }

    @NotNull
    public static FederatedCacheNode createNode() {
        return new FederatedCacheNode("testnode" + UUID.randomUUID(), "http://test.com", List.of(TEST_PROTOCOL));
    }
}
