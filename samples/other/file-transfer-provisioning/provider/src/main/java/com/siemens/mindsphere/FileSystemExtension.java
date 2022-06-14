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
 *
 */

package com.siemens.mindsphere;

import com.siemens.mindsphere.provision.FileSystemProvisionedResource;
import com.siemens.mindsphere.provision.FileSystemProvisioner;
import com.siemens.mindsphere.provision.FileSystemResourceDefinition;
import com.siemens.mindsphere.provision.FileSystemResourceDefinitionGenerator;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema;

import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;

public class FileSystemExtension implements ServiceExtension {
    private static final Action USE_ACTION = Action.Builder.newInstance().type("USE").build();

    public static final String USE_POLICY = "use-eu";

    @EdcSetting
    private static final String EDC_ASSET_PATH = "edc.samples.04.asset.path";

    @EdcSetting
    private static final String EDC_ASSET_URL = "edc.samples.04.asset.url";

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private ContractDefinitionStore contractStore;

    @Inject
    private AssetLoader loader;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;


    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();

        // register provisioner
        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var provisionManager = context.getService(ProvisionManager.class);
        final FileSystemProvisioner fileSystemProvisioner = new FileSystemProvisioner(monitor, retryPolicy);
        provisionManager.register(fileSystemProvisioner);

        var policy = createPolicy();
        policyStore.save(policy);

        registerDataEntries1(context);
        registerContractDefinition1(policy.getUid());

        registerDataEntries2(context);
        registerContractDefinition2(policy.getUid());

        registerDataEntries3(context);
        registerContractDefinition3(policy.getUid());

        // register the fs resource definition generator
        manifestGenerator.registerGenerator(new FileSystemResourceDefinitionGenerator(monitor, context));

        // register provision specific classes
        registerTypes(context.getTypeManager());
    }

    @Override
    public String name() {
        return "FileSystem Transfer With Provisioning";
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(FileSystemProvisionedResource.class, FileSystemResourceDefinition.class);
    }

    private PolicyDefinition createPolicy() {
        var usePermission = Permission.Builder.newInstance()
                .action(USE_ACTION)
                .build();

        return PolicyDefinition.Builder.newInstance()
                .uid(USE_POLICY)
                .policy(Policy.Builder.newInstance()
                        .permission(usePermission)
                        .build())
                .build();
    }

    private void registerDataEntries1(ServiceExtensionContext context) {
        var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "d:/edc/provider/warren.jpg");

        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", "file")
                .property("path", assetPathSetting)
                .build();

        var assetId = "warren";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition1(String uid) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(uid)
                .contractPolicyId(uid)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "warren").build())
                .build();

        contractStore.save(contractDefinition);
    }

    private void registerDataEntries2(ServiceExtensionContext context) {
        var assetPathSetting = context.getSetting(EDC_ASSET_URL, "https://docs.oracle.com/javaee/5/firstcup/doc/firstcup.pdf");

        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", HttpDataAddressSchema.TYPE)
                .property("http", assetPathSetting)
                .property(NAME, "")
                .property(ENDPOINT, assetPathSetting)
                .build();

        var assetId = "onboarding";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition2(String uid) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicyId(uid)
                .contractPolicyId(uid)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "onboarding").build())
                .build();

        contractStore.save(contractDefinition);
    }

    private void registerDataEntries3(ServiceExtensionContext context) {
        var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "data/ten=castidev/data.csv");

        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", HttpDataAddressSchema.TYPE)
                .property("datalakepath", assetPathSetting)
                .build();

        var assetId = "data.csv";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition3(String uid) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("3")
                .accessPolicyId(uid)
                .contractPolicyId(uid)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "data.csv").build())
                .build();

        contractStore.save(contractDefinition);
    }
}
