/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import okhttp3.Interceptor;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.retry.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerExtension;
import org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerWebhookUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.PROVISIONER_CONFIG;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.TEST_DATA_TYPE;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.createResponse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({ EdcExtension.class })
public class HttpProvisionerExtensionEndToEndTest {
    private static final String ASSET_ID = "1";
    private static final String CONTRACT_ID = "2";
    private static final String POLICY_ID = "3";
    private final int dataPort = getFreePort();

    private Interceptor delegate;

    /**
     * Tests the case where an initial request returns a retryable failure and the second request completes.
     */
    @Test
    void processProviderRequestRetry(TransferProcessManager processManager,
                                     ContractNegotiationStore negotiationStore,
                                     AssetLoader loader,
                                     TransferProcessStore store, PolicyDefinitionStore policyStore) throws Exception {
        var latch = new CountDownLatch(1);

        when(delegate.intercept(any()))
                .thenAnswer(invocation -> createResponse(503, invocation))
                .thenAnswer(invocation -> {
                    latch.countDown();
                    return createResponse(200, invocation);
                });

        loadNegotiation(negotiationStore, policyStore);

        loadAsset(loader);

        var result = processManager.initiateProviderRequest(createRequest());

        assertThat(latch.await(10000, MILLISECONDS)).isTrue();

        var transferProcess = store.find(result.getContent());

        assertThat(transferProcess).isNotNull();
        assertThat(transferProcess.getState()).isEqualTo(PROVISIONING.code());
    }

    @BeforeEach
    void setup(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(dataPort),
                "web.http.data.path", "/api/v1/data"
        ));
        delegate = mock(Interceptor.class);
        var httpClient = testOkHttpClient().newBuilder().addInterceptor(delegate).build();

        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerSystemExtension(ServiceExtension.class, new HttpProvisionerExtension(httpClient));
        extension.registerSystemExtension(ServiceExtension.class, new DummyCallbackUrlExtension());
        extension.setConfiguration(PROVISIONER_CONFIG);
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            @Inject
            private AssetLoader loader; //needed for on-demand dependency resolution
        });
    }

    private void loadNegotiation(ContractNegotiationStore negotiationStore, PolicyDefinitionStore policyStore) {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .assetId(ASSET_ID)
                .id(CONTRACT_ID)
                .policy(Policy.Builder.newInstance().build())
                .consumerAgentId("consumer")
                .providerAgentId("provider")
                .build();

        var contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("test")
                .protocol("test")
                .contractAgreement(contractAgreement)
                .build();
        negotiationStore.save(contractNegotiation);

        policyStore.save(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).uid(POLICY_ID).build());
    }

    private void loadAsset(AssetLoader loader) {
        var asset = Asset.Builder.newInstance().id(ASSET_ID).build();
        var dataAddress = DataAddress.Builder.newInstance().type(TEST_DATA_TYPE).build();

        // load the asset
        loader.accept(asset, dataAddress);
    }

    private DataRequest createRequest() {
        return DataRequest.Builder.newInstance().destinationType("test").assetId(ASSET_ID).contractId(CONTRACT_ID).build();
    }

    @Provides(HttpProvisionerWebhookUrl.class)
    private static class DummyCallbackUrlExtension implements ServiceExtension {
        @Override
        public void initialize(ServiceExtensionContext context) {
            try {
                var url = new URL("http://localhost:8080");
                context.registerService(HttpProvisionerWebhookUrl.class, () -> url);
            } catch (MalformedURLException e) {
                fail(e);
            }

        }
    }
}