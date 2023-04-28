/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.test.extension.api;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Extension(FileTransferExtension.NAME)
public class FileTransferExtension implements ServiceExtension {
    public static final String NAME = "File Transfer";

    public static final String USE_POLICY = "use-eu";
    private static final String EDC_ASSET_PATH = "edc.test.asset.path";
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private DataTransferExecutorServiceContainer executorContainer;
    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private Telemetry telemetry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sourceFactory = new FileTransferDataSourceFactory();
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new FileTransferDataSinkFactory(monitor, telemetry, executorContainer.getExecutorService(), 5);
        pipelineService.registerFactory(sinkFactory);

        var policy = createPolicy();
        policyStore.create(policy);

        registerDataEntries(context);
        registerContractDefinition(policy.getUid());

        context.getMonitor().info("File Transfer Extension initialized!");
    }

    private PolicyDefinition createPolicy() {

        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance()
                        .permission(usePermission)
                        .target("test-document")
                        .build())
                .id(USE_POLICY)
                .build();
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "/tmp/provider/test-document.txt");
        var assetPath = Path.of(assetPathSetting);

        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", "File")
                .property("path", assetPath.getParent().toString())
                .property("filename", assetPath.getFileName().toString())
                .build();

        var assetId = "test-document";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        assetIndex.accept(asset, dataAddress);
    }

    private void registerContractDefinition(String policyId) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "test-document").build())
                .validity(TimeUnit.HOURS.toSeconds(1))
                .build();

        contractStore.save(contractDefinition);
    }
}
