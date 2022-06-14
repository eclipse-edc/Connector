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
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.transfer.core.inline.InlineDataFlowController;

import java.util.UUID;

public class TransferExtension implements ServiceExtension {
    @EdcSetting
    private static final String STUB_URL = "edc.demo.http.source.url";

    @Inject
    private DataFlowManager dataFlowMgr;
    @Inject
    private DataAddressResolver dataAddressResolver;
    @Inject
    private DataOperatorRegistry dataOperatorRegistry;
    @Inject
    private ContractDefinitionLoader contractDefinitionLoader;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private AssetLoader assetLoader;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var monitor = context.getMonitor();

        //Comment the below 3 lines out to remove the old way of copying files
        dataOperatorRegistry.registerWriter(new HttpWriter(monitor));
        dataOperatorRegistry.registerReader(new HttpReader(monitor));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry));

        String assetId = registerDataEntries(context);
        monitor.info("Register http sample Asset: " + assetId);

        var policy = createPolicy(assetId);
        policyStore.save(policy);

        registerContractDefinition(policy.getUid(), assetId);
        context.getMonitor().info("HTTP Transfer Extension initialized!");
    }

    private PolicyDefinition createPolicy(String assetId) {
        var policy = Policy.Builder.newInstance()
                .target(assetId)
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        return PolicyDefinition.Builder.newInstance()
                .policy(policy)
                .build();
    }

    private String registerDataEntries(ServiceExtensionContext context) {
        final String assetUrl = context.getSetting(STUB_URL, "missing");

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type(HttpSchema.TYPE)
                .property(HttpSchema.URL, assetUrl)
                .keyName("demo.jpg")
                .build();

        var assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance().id(assetId).build();
        assetLoader.accept(asset, dataAddress);

        return assetId;
    }

    private void registerContractDefinition(String policyId, String assetId) {
        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance()
                        .whenEquals(Asset.PROPERTY_ID, assetId).build())
                .build();

        contractDefinitionLoader.accept(contractDefinition1);
    }

}
