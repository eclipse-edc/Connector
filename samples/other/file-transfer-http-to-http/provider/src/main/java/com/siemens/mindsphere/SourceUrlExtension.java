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


import com.siemens.mindsphere.datalake.edc.http.DataLakeClientImpl;
import com.siemens.mindsphere.datalake.edc.http.OauthClientDetails;
import com.siemens.mindsphere.datalake.edc.http.dataplane.DatalakeHttpDataSinkFactory;
import com.siemens.mindsphere.datalake.edc.http.provision.*;
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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
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

import java.net.URL;

public class SourceUrlExtension implements ServiceExtension {

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private OkHttpClient httpClient;

    @EdcSetting
    private static final String TOKENMANAGEMENT_ADDRESS = "datalake.oauth.address";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_ID = "datalake.oauth.client.id";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_SECRET = "datalake.oauth.client.secret";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_NAME = "datalake.oauth.client.app.name";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_VERSION = "datalake.oauth.client.app.version";

    @EdcSetting
    private static final String APPLICATION_TENANT = "datalake.tenant";

    @EdcSetting
    private static final String DATALAKE_BASE_URL = "datalake.base.url.endpoint";

    @Inject
    private PolicyStore policyStore;

    @Inject
    private ContractDefinitionStore contractStore;

    @Inject
    private AssetLoader loader;

    private static final String USE_POLICY = "use-eu";

    @EdcSetting
    private static final String EDC_ASSET_PATH = "edc.sample.path";

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();

        try {

            final String tokenmanagementClientId = context.getSetting(TOKENMANAGEMENT_CLIENT_ID, "");
            final String tokenmanagementClientSecret = context.getSetting(TOKENMANAGEMENT_CLIENT_SECRET, "");
            final String tokenmanagementClientAppName = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_NAME, "");
            final String tokenmanagementClientAppVersion = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_VERSION, "");
            final String tokenmanagementAddress = context.getSetting(TOKENMANAGEMENT_ADDRESS, "");
            final String dataLakeAddress = context.getSetting(DATALAKE_BASE_URL, "");

            final String applicationTenant = context.getSetting(APPLICATION_TENANT, "presdev");

            monitor.debug("datalake.oauth.clientId=" + tokenmanagementClientId.substring(0, 5));
            monitor.debug("datalake.oauth.clientSecret=" + tokenmanagementClientSecret.substring(0, 5));
            monitor.debug("datalake.tenant=" + applicationTenant);
            monitor.debug("datalake.address=" + dataLakeAddress);

            final URL url = new URL(dataLakeAddress);

            final OauthClientDetails oauthClientDetails = new OauthClientDetails(tokenmanagementClientId, tokenmanagementClientSecret,
                    tokenmanagementClientAppName, tokenmanagementClientAppVersion, applicationTenant, new URL(tokenmanagementAddress));
            final DataLakeClientImpl clientImpl = new DataLakeClientImpl(oauthClientDetails, url);

            @SuppressWarnings("unchecked") var retryPolicy = context.getService(RetryPolicy.class);
            var provisionManager = context.getService(ProvisionManager.class);
            final DestinationUrlProvisioner sourceUrlProvisioner = new DestinationUrlProvisioner(clientImpl, monitor, retryPolicy);
            provisionManager.register(sourceUrlProvisioner);

            //MUST BE 1 partition - we can handle only one part at a time
            var sinkFactoryHttp = new DatalakeHttpDataSinkFactory(httpClient, executorContainer.getExecutorService(), 1, monitor);
            pipelineService.registerFactory(sinkFactoryHttp);

            // register the datalake resource definition generator
            manifestGenerator.registerGenerator(new SourceUrlResourceDefinitionGenerator(monitor, context));

            // register provision specific classes
            registerTypes(context.getTypeManager());
        } catch (Exception e) {
            monitor.severe("Failed to register datalake source url provisioning");
        }

        addTestData(context);
    }

    /**
     * Added only for testing
     * Use the below curl commands to test
     *
     * curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/file-transfer-http-to-http/datalakecontractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"
     *
     * curl -X GET -H "Content-Type: application/json" -H "X-Api-Key: password"  "http://localhost:9192/api/v1/data/contractnegotiations"
     *
     * curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/file-transfer-http-to-http/datalaketransfer.json "http://localhost:9192/api/v1/data/transferprocess"
     *
     * @param context
     */
    private void addTestData(ServiceExtensionContext context) {
        var policy = createPolicy();
        policyStore.save(policy);

        registerDataEntries(context);
        registerContractDefinition(policy.getUid());
    }

    @Override
    public String name() {
        return "Datalake Transfer With Provisioning";
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(SourceUrlProvisionedResource.class, SourceUrlResourceDefinition.class);
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
        var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "data/ten=castidev/data.csv");

        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", HttpDataAddressSchema.TYPE)
                .property(MindsphereSchema.DATALAKE_PATH, assetPathSetting)
                .build();

        var assetId = "data.csv";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition(String uid) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("9")
                .accessPolicyId(uid)
                .contractPolicyId(uid)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "data.csv").build())
                .build();

        contractStore.save(contractDefinition);
    }
}
