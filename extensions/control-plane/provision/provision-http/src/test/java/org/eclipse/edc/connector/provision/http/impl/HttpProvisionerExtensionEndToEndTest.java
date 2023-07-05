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

package org.eclipse.edc.connector.provision.http.impl;

import okhttp3.Interceptor;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.provision.http.HttpProvisionerWebhookUrl;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.provision.http.HttpProvisionerFixtures.PROVISIONER_CONFIG;
import static org.eclipse.edc.connector.provision.http.HttpProvisionerFixtures.TEST_DATA_TYPE;
import static org.eclipse.edc.connector.provision.http.HttpProvisionerFixtures.createResponse;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(EdcExtension.class)
public class HttpProvisionerExtensionEndToEndTest {
    private static final String ASSET_ID = "assetId";
    private static final String CONTRACT_ID = ContractId.create("definitionId", ASSET_ID).toString();
    private static final String POLICY_ID = "3";
    private final int dataPort = getFreePort();
    private final Interceptor delegate = mock(Interceptor.class);
    private final ContractValidationService contractValidationService = mock(ContractValidationService.class);

    @BeforeEach
    void setup(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(dataPort),
                "web.http.management.path", "/api/v1/management"
        ));

        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerServiceMock(EdcHttpClient.class, testHttpClient(delegate));
        extension.registerServiceMock(ContractValidationService.class, contractValidationService);
        extension.registerServiceMock(ProtocolWebhook.class, mock(ProtocolWebhook.class));
        extension.registerSystemExtension(ServiceExtension.class, new DummyCallbackUrlExtension());
        extension.setConfiguration(PROVISIONER_CONFIG);
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            @Inject
            private AssetIndex assetIndex; // needed for on-demand dependency resolution
        });
    }

    /**
     * Tests the case where an initial request returns a retryable failure and the second request completes.
     */
    @Test
    void processProviderRequestRetry(TransferProcessProtocolService protocolService,
                                     ContractNegotiationStore negotiationStore,
                                     AssetIndex assetIndex,
                                     TransferProcessStore store, PolicyDefinitionStore policyStore) throws Exception {
        when(contractValidationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        negotiationStore.save(createContractNegotiation());
        policyStore.create(createPolicyDefinition());
        assetIndex.create(createAssetEntry());

        when(delegate.intercept(any()))
                .thenAnswer(invocation -> createResponse(503, invocation))
                .thenAnswer(invocation -> createResponse(200, invocation));

        var result = protocolService.notifyRequested(createTransferRequestMessage(), ClaimToken.Builder.newInstance().build());

        assertThat(result).isSucceeded();
        await().untilAsserted(() -> {
            var transferProcess = store.findById(result.getContent().getId());
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).isEqualTo(PROVISIONING_REQUESTED.code());
        });
    }

    private PolicyDefinition createPolicyDefinition() {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id(POLICY_ID).build();
    }

    private ContractNegotiation createContractNegotiation() {
        var policy = Policy.Builder.newInstance().build();
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .assetId(ASSET_ID)
                .id(CONTRACT_ID)
                .policy(policy)
                .consumerId("consumer")
                .providerId("provider")
                .build();

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(randomUUID().toString())
                .assetId(randomUUID().toString())
                .policy(policy)
                .build();
        return ContractNegotiation.Builder.newInstance()
                .id(randomUUID().toString())
                .counterPartyId(randomUUID().toString())
                .counterPartyAddress("test")
                .protocol("test")
                .contractAgreement(contractAgreement)
                .contractOffer(contractOffer)
                .build();
    }

    @NotNull
    private Asset createAssetEntry() {
        return Asset.Builder.newInstance()
                .id(ASSET_ID)
                .dataAddress(DataAddress.Builder.newInstance().type(TEST_DATA_TYPE).build())
                .build();
    }

    private TransferRequestMessage createTransferRequestMessage() {
        return TransferRequestMessage.Builder.newInstance()
                .processId(randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                .protocol("any")
                .counterPartyAddress("http://any")
                .callbackAddress("http://any")
                .contractId(CONTRACT_ID)
                .build();
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
