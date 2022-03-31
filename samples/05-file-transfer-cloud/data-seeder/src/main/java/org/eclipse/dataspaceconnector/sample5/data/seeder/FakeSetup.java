/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sample5.data.seeder;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FakeSetup {

    private final Monitor monitor;
    private final AssetLoader assetIndexLoader;
    private final ContractDefinitionStore contractDefinitionStore;

    public FakeSetup(@NotNull Monitor monitor, @NotNull AssetLoader assetIndexLoader, @NotNull ContractDefinitionStore contractDefinitionStore) {
        this.monitor = Objects.requireNonNull(monitor);
        this.assetIndexLoader = Objects.requireNonNull(assetIndexLoader);
        this.contractDefinitionStore = Objects.requireNonNull(contractDefinitionStore);
    }

    public void setupAssets() {
        Asset asset = Asset.Builder.newInstance().id("1").build();
        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "<storage-account-name>")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .keyName("<storage-account-name>-key1")
                .build();
        assetIndexLoader.accept(asset, dataAddress);

        Asset asset2 = Asset.Builder.newInstance().id("2").build();
        DataAddress dataAddress2 = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "<storage-account-name>")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .keyName("<storage-account-name>-key1")
                .build();
        assetIndexLoader.accept(asset2, dataAddress2);

        monitor.info("Register Blob Stored Sample Asset: 1");
    }

    public void setupContractOffers() {
        Policy publicPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        Policy publicPolicy2 = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(publicPolicy)
                .contractPolicy(publicPolicy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "1").build())
                .build();

        ContractDefinition contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicy(publicPolicy2)
                .contractPolicy(publicPolicy2)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "2").build())
                .build();

        contractDefinitionStore.save(contractDefinition1);
        contractDefinitionStore.save(contractDefinition2);
    }
}
