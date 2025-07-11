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

package org.eclipse.edc.connector.controlplane.provision.http.impl;

import okhttp3.Interceptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.provision.http.HttpProvisionerFixtures;
import org.eclipse.edc.connector.controlplane.provision.http.HttpProvisionerWebhookUrl;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.RequestTransferContext;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.ProtocolWebhook;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(RuntimePerMethodExtension.class)
public class HttpProvisionerExtensionEndToEndTest {
    private static final String ASSET_ID = "assetId";
    private static final String CONTRACT_ID = UUID.randomUUID().toString();
    private static final String POLICY_ID = "3";
    private final int dataPort = getFreePort();
    private final Interceptor delegate = mock(Interceptor.class);
    private final ContractValidationService contractValidationService = mock();
    private final IdentityService identityService = mock();

    @BeforeEach
    void setup(RuntimeExtension extension) {
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
        extension.registerServiceMock(IdentityService.class, identityService);
        extension.registerServiceMock(DataPlaneClientFactory.class, mock());
        var dataAddressValidatorRegistry = mock(DataAddressValidatorRegistry.class);
        when(dataAddressValidatorRegistry.validateSource(any())).thenReturn(ValidationResult.success());
        when(dataAddressValidatorRegistry.validateDestination(any())).thenReturn(ValidationResult.success());
        extension.registerServiceMock(DataAddressValidatorRegistry.class, dataAddressValidatorRegistry);
        extension.registerSystemExtension(ServiceExtension.class, new DummyCallbackUrlExtension());
        extension.setConfiguration(HttpProvisionerFixtures.PROVISIONER_CONFIG);
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
        when(contractValidationService.validateAgreement(any(ParticipantAgent.class), any())).thenReturn(Result.success(null));
        var policy = Policy.Builder.newInstance().build();
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .assetId(ASSET_ID)
                .id(CONTRACT_ID)
                .policy(policy)
                .consumerId("consumer")
                .providerId("provider")
                .build();

        negotiationStore.save(createContractNegotiation(contractAgreement, policy));
        policyStore.create(createPolicyDefinition());
        assetIndex.create(createAssetEntry());

        when(delegate.intercept(any()))
                .thenAnswer(invocation -> HttpProvisionerFixtures.createResponse(503, invocation))
                .thenAnswer(invocation -> HttpProvisionerFixtures.createResponse(200, invocation));

        when(identityService.verifyJwtToken(any(), isA(VerificationContext.class))).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = protocolService.notifyRequested(createTransferRequestMessage(), new ParticipantAgent(emptyMap(), emptyMap()), new RequestTransferContext(contractAgreement));

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

    private ContractNegotiation createContractNegotiation(ContractAgreement contractAgreement, Policy policy) {
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
                .dataAddress(DataAddress.Builder.newInstance().type(HttpProvisionerFixtures.TEST_DATA_TYPE).build())
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
