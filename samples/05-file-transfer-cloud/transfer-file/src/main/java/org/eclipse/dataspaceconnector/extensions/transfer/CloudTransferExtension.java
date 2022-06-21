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

package org.eclipse.dataspaceconnector.extensions.transfer;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

public class CloudTransferExtension implements ServiceExtension {
    @Inject
    private AssetLoader loader;
    @Inject
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Cloud-Based Transfer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var policy = createPolicy();
        policyDefinitionStore.save(policy);

        registerDataEntries();
        registerContractDefinition(policy.getUid());
    }

    public void registerDataEntries() {
        var asset = Asset.Builder.newInstance().id("1").build();
        var dataAddress = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "<storage-account-name>")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .keyName("<storage-account-name>-key1")
                .build();
        loader.accept(asset, dataAddress);

        var asset2 = Asset.Builder.newInstance().id("2").build();
        var dataAddress2 = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "<storage-account-name>")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .keyName("<storage-account-name>-key1")
                .build();
        loader.accept(asset2, dataAddress2);
    }

    public void registerContractDefinition(String policyId) {
        var contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "1").build())
                .build();

        var contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "2").build())
                .build();

        contractDefinitionStore.save(contractDefinition1);
        contractDefinitionStore.save(contractDefinition2);
    }

    private PolicyDefinition createPolicy() {
        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(usePermission)
                        .build())
                .build();
    }
}
