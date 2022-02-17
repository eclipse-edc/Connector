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
package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderContractNegotiationManagerImplTest {

    private ContractValidationService validationService;
    private ProviderContractNegotiationManagerImpl negotiationManager;

    private final Map<String, ContractNegotiation> negotiations = new HashMap<>();

    private final String correlationId = "correlationId";

    @BeforeEach
    void setUp() throws Exception {
        negotiations.clear();

        // Mock contract negotiation store -> persist negotiations in map
        ContractNegotiationStore negotiationStore = mock(ContractNegotiationStore.class);

        doAnswer(invocation -> {
            var negotiation = invocation.getArgument(0, ContractNegotiation.class);
            negotiations.put(negotiation.getId(), negotiation);
            return null;
        }).when(negotiationStore).save(any(ContractNegotiation.class));
        when(negotiationStore.find(anyString())).thenAnswer(invocation -> {
            var id = invocation.getArgument(0, String.class);
            return negotiations.get(id);
        });
        when(negotiationStore.findForCorrelationId(anyString())).thenAnswer(invocation -> {
            var id = invocation.getArgument(0, String.class);
            for (Map.Entry<String, ContractNegotiation> entry : negotiations.entrySet()) {
                if (entry.getValue().getCorrelationId().equals(id)) {
                    return entry.getValue();
                }
            }
            return null;
        });

        // Create contract validation service mock, method mocking has to be done in test methods
        validationService = mock(ContractValidationService.class);

        // Create dispatcher registry mock in order to create manager, not used in unit tests
        RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);

        // Create monitor mock
        Monitor monitor = mock(Monitor.class);
        
        // Create CommandQueue mock
        CommandQueue<ContractNegotiationCommand> queue = (CommandQueue<ContractNegotiationCommand>) mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());
        
        // Create CommandRunner mock
        CommandRunner<ContractNegotiationCommand> runner = (CommandRunner<ContractNegotiationCommand>) mock(CommandRunner.class);

        negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(mock(ContractNegotiationObservable.class))
                .build();

        //TODO hand over store in start method, but run method should not be executed
        var negotiationStoreField = ProviderContractNegotiationManagerImpl.class.getDeclaredField("negotiationStore");
        negotiationStoreField.setAccessible(true);
        negotiationStoreField.set(negotiationManager, negotiationStore);
    }

    @Test
    void testRequestedConfirmOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.success(contractOffer));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = negotiationManager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMING.code());
        assertThat(negotiation.getCounterPartyId()).isEqualTo(request.getConnectorId());
        assertThat(negotiation.getCounterPartyAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(negotiation.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(negotiation.getCorrelationId()).isEqualTo(request.getCorrelationId());
        assertThat(negotiation.getContractOffers()).hasSize(1);
        assertThat(negotiation.getLastContractOffer()).isEqualTo(contractOffer);
        verify(validationService).validate(token, contractOffer);
    }

    @Test
    void testRequestedDeclineOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.failure("error"));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = negotiationManager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getCounterPartyId()).isEqualTo(request.getConnectorId());
        assertThat(negotiation.getCounterPartyAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(negotiation.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(negotiation.getCorrelationId()).isEqualTo(request.getCorrelationId());
        assertThat(negotiation.getContractOffers()).hasSize(1);
        assertThat(negotiation.getLastContractOffer()).isEqualTo(contractOffer);
        verify(validationService).validate(token, contractOffer);
    }

    @Test
    @Disabled
    void testRequestedCounterOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        var counterOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.success(null));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = negotiationManager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.PROVIDER_OFFERING.code());
        assertThat(negotiation.getCounterPartyId()).isEqualTo(request.getConnectorId());
        assertThat(negotiation.getCounterPartyAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(negotiation.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(negotiation.getCorrelationId()).isEqualTo(request.getCorrelationId());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(0)).isEqualTo(contractOffer);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(counterOffer);
        verify(validationService).validate(token, contractOffer);
    }

    @Test
    void testOfferReceivedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();

        var result = negotiationManager.offerReceived(token, "not a valid id", contractOffer, "hash");
        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testOfferReceivedConfirmOffer() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();

        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(contractOffer));

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.failure("error"));

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    @Disabled
    void testOfferReceivedCounterOffer() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        var counterOffer = contractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(null));

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.PROVIDER_OFFERING.code());
        assertThat(negotiation.getContractOffers()).hasSize(3);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        assertThat(negotiation.getContractOffers().get(2)).isEqualTo(counterOffer);
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testConsumerApprovedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = (ContractAgreement) mock(ContractAgreement.class);

        var result = negotiationManager.consumerApproved(token, "not a valid id", contractAgreement, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConsumerApprovedConfirmAgreement() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = (ContractAgreement) mock(ContractAgreement.class);

        var result = negotiationManager.consumerApproved(token, correlationId, contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMING.code());
        assertThat(negotiation.getContractAgreement()).isNull();
    }

    @Test
    void testDeclined() {
        var negotiationId = createContractNegotiationProviderOffered();
        var token = ClaimToken.Builder.newInstance().build();

        var result = negotiationManager.declined(token, correlationId);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINED.code());
    }

    private String createContractNegotiationProviderOffered() {
        var lastOffer = contractOffer();

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(400)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .type(ContractNegotiation.Type.PROVIDER)
                .build();
        negotiation.addContractOffer(lastOffer);
        negotiations.put(negotiation.getId(), negotiation);
        return negotiation.getId();
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance().id("id").policy(Policy.Builder.newInstance().build()).build();
    }

}
