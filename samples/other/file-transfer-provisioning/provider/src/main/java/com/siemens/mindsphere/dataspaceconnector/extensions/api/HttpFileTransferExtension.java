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

package com.siemens.mindsphere.dataspaceconnector.extensions.api;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema;

import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;

public class HttpFileTransferExtension implements ServiceExtension {

    public static final String USE_POLICY = "use-eu";

    @EdcSetting
    private static final String EDC_ASSET_URL = "edc.samples.04.asset.url";

    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetLoader loader;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private DataTransferExecutorServiceContainer executorContainer;
    @Inject
    private PolicyStore policyStore;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @SuppressWarnings("rawtypes")
    private RetryPolicy retryPolicy;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sinkFactory = new FileTransferDataSinkFactory(monitor, executorContainer.getExecutorService(), 5);
        pipelineService.registerFactory(sinkFactory);

        //MUST BE 1 partition - we can handle only one part at a time
        var sinkFactoryHttp = new HttpDataSinkFactory(httpClient, executorContainer.getExecutorService(), 1, monitor);
        pipelineService.registerFactory(sinkFactoryHttp);

        var policy = createPolicy();
        policyStore.save(policy);

        registerDataEntries(context);
        registerContractDefinition(policy.getUid());

        context.getMonitor().info("Http File Transfer Extension initialized!");
    }

    private Policy createPolicy() {

        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return Policy.Builder.newInstance()
                .id(USE_POLICY)
                .permission(usePermission)
                .build();
    }

    private void registerDataEntries(ServiceExtensionContext context) {
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

    private void registerContractDefinition(String uid) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicyId(uid)
                .contractPolicyId(uid)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "onboarding").build())
                .build();

        contractStore.save(contractDefinition);
    }
}
