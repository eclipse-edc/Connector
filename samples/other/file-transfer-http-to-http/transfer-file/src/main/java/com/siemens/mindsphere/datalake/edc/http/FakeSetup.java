/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

public class FakeSetup {
    public FakeSetup(Monitor monitor, AssetLoader assetIndexLoader,
            ContractDefinitionLoader contractDefinitionLoader, PolicyStore policyStore, String url) {
        this.monitor = monitor;
        this.assetIndexLoader = assetIndexLoader;
        this.contractDefinitionLoader = contractDefinitionLoader;
        this.policyStore = policyStore;
        this.url = url;
    }

    private final Monitor monitor;
    private final AssetLoader assetIndexLoader;
    private final ContractDefinitionLoader contractDefinitionLoader;
    private final PolicyStore policyStore;
    private final String url;

    public void setupAssets() {
        Asset asset = Asset.Builder.newInstance().id("1").build();
        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type(HttpSchema.TYPE)
                .property(HttpSchema.URL, url)
                .keyName("demo.jpg")
                .build();
        assetIndexLoader.accept(asset, dataAddress);

        monitor.info("Register http sample Asset: 1");
    }

    public void setupContractOffers() {
        Policy publicPolicy = Policy.Builder.newInstance()
                .target("1")
                .permission(Permission.Builder.newInstance()
                        .target("1")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        policyStore.save(publicPolicy);

        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(publicPolicy.getUid())
                .contractPolicyId(publicPolicy.getUid())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance()
                        .whenEquals(Asset.PROPERTY_ID, "1").build())
                .build();

        contractDefinitionLoader.accept(contractDefinition1);

    }
}
