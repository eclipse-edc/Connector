/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.junit;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class EndToEndTest {
    private static final String ASSET_ID = "test123";
    private static final String CONTRACT_ID = "contract1";
    private static final String POLICY_ID = "policy1";

    @Test
    void processConsumerRequest(TransferProcessManager processManager, RemoteMessageDispatcherRegistry dispatcherRegistry) throws InterruptedException {
        var latch = new CountDownLatch(1);

        var dispatcher = mock(RemoteMessageDispatcher.class);

        when(dispatcher.protocol()).thenReturn(Protocols.IDS_MULTIPART);

        when(dispatcher.send(notNull(), isA(RemoteMessage.class), isA(MessageContext.class))).thenAnswer(i -> {
            latch.countDown();
            return CompletableFuture.completedFuture(null);
        });

        dispatcherRegistry.register(dispatcher);

        var connectorId = "https://test";

        var entry = Asset.Builder.newInstance().id(ASSET_ID).build();
        var request = DataRequest.Builder.newInstance().protocol(Protocols.IDS_MULTIPART).assetId(entry.getId())
                .connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").build();

        processManager.initiateConsumerRequest(request);

        assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
        verify(dispatcher).protocol();
        verify(dispatcher).send(notNull(), isA(RemoteMessage.class), isA(MessageContext.class));
    }

    @Test
    void processProviderRequest(TransferProcessManager processManager,
                                DataFlowManager dataFlowManager,
                                ContractNegotiationStore negotiationStore,
                                AssetLoader loader,
                                PolicyDefinitionStore policyStore) throws InterruptedException {
        var latch = new CountDownLatch(1);

        var controllerMock = mock(DataFlowController.class);

        when(controllerMock.canHandle(isA(DataRequest.class), isA(DataAddress.class))).thenReturn(true);
        when(controllerMock.initiateFlow(isA(DataRequest.class), isA(DataAddress.class), isA(Policy.class))).thenAnswer(i -> {
            latch.countDown();
            return StatusResult.success("");
        });

        dataFlowManager.register(controllerMock);

        var connectorId = "https://test";

        var asset = Asset.Builder.newInstance().id(ASSET_ID).build();

        loader.accept(asset, DataAddress.Builder.newInstance().type("test").build());

        loadNegotiation(negotiationStore, policyStore);

        var request = DataRequest.Builder.newInstance()
                .protocol(Protocols.IDS_MULTIPART)
                .assetId(asset.getId())
                .contractId(CONTRACT_ID)
                .connectorId(connectorId)
                .connectorAddress(connectorId)
                .destinationType("S3")
                .id(UUID.randomUUID().toString())
                .build();

        processManager.initiateProviderRequest(request);

        assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
        verify(controllerMock).canHandle(isA(DataRequest.class), isA(DataAddress.class));
        verify(controllerMock).initiateFlow(isA(DataRequest.class), isA(DataAddress.class), isA(Policy.class));
    }

    @BeforeEach
    void before(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new TestServiceExtension());
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

    @Provides(IdentityService.class)
    private static class TestServiceExtension implements ServiceExtension {
        @Inject
        private AssetLoader loader;

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(IdentityService.class, new IdentityService() {
                @Override
                public Result<TokenRepresentation> obtainClientCredentials(String scope, String audience) {
                    return Result.success(TokenRepresentation.Builder.newInstance().token("test").build());
                }

                @Override
                public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
                    return Result.success(ClaimToken.Builder.newInstance().build());
                }
            });
        }
    }
}
