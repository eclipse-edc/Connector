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
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerContractNegotiationManagerImplTest {

    private ContractValidationService validationService;
    private ConsumerContractNegotiationManagerImpl negotiationManager;

    private final Map<String, ContractNegotiation> negotiations = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        negotiations.clear();

        // Mock contract negotiation store -> persist negotiations in map
        ContractNegotiationStore negotiationStore = mock(ContractNegotiationStore.class);
        doAnswer(invocation -> {
            var negotiation = invocation.getArgument(0, ContractNegotiation.class);
            if (ContractNegotiationStates.UNSAVED.code() == negotiation.getState()) {
                negotiation.transitionRequesting();
            }
            negotiations.put(negotiation.getId(), negotiation);
            return null;
        }).when(negotiationStore).save(any(ContractNegotiation.class));

        when(negotiationStore.find(anyString())).thenAnswer(invocation -> {
            var id = invocation.getArgument(0, String.class);
            return negotiations.get(id);
        });

        // Create contract validation service mock, method mocking has to be done in test methods
        validationService = mock(ContractValidationService.class);

        // Create dispatcher registry mock in order to create manager, not used in unit tests
        RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);

        // Create monitor mock
        Monitor monitor = mock(Monitor.class);

        negotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .build();

        //TODO hand over store in start method, but run method should not be executed
        var negotiationStoreField = ConsumerContractNegotiationManagerImpl.class.getDeclaredField("negotiationStore");
        negotiationStoreField.setAccessible(true);
        negotiationStoreField.set(negotiationManager, negotiationStore);
    }

    @Test
    void testInitiate() {
        var contractOffer = aContractOffer();

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .build();

        var result = negotiationManager.initiate(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.REQUESTING.code());
        assertThat(negotiation.getCounterPartyId()).isEqualTo(request.getConnectorId());
        assertThat(negotiation.getCounterPartyAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(negotiation.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(negotiation.getCorrelationId()).isNull();
        assertThat(negotiation.getContractOffers()).hasSize(1);
        assertThat(negotiation.getLastContractOffer()).isEqualTo(contractOffer);
    }

    @Test
    void testOfferReceivedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = aContractOffer();

        var result = negotiationManager.offerReceived(token, "not a valid id", contractOffer, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testOfferReceivedConfirmOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = aContractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(contractOffer));

        var result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONSUMER_APPROVING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = aContractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.failure("error"));

        var result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
    }

    @Test
    @Disabled
    void testOfferReceivedCounterOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = aContractOffer();
        var counterOffer = aContractOffer();

        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(null));

        var result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONSUMER_OFFERING.code());
        assertThat(negotiation.getContractOffers()).hasSize(3);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        assertThat(negotiation.getContractOffers().get(2)).isEqualTo(counterOffer);
    }

    @Test
    void testConfirmedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);

        var result = negotiationManager.confirmed(token, "not a valid id", contractAgreement, "hash");
        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConfirmedConfirmAgreement() {
        var negotiationId = createContractNegotiationConsumerOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);

        when(validationService.validate(eq(token), eq(contractAgreement), any(ContractOffer.class)))
                .thenReturn(true);

        var result = negotiationManager.confirmed(token, negotiationId, contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMED.code());
        assertThat(negotiation.getContractAgreement()).isEqualTo(contractAgreement);
    }

    @Test
    void testConfirmedDeclineAgreement() {
        var negotiationId = createContractNegotiationConsumerOffered();

        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);

        when(validationService.validate(eq(token), eq(contractAgreement), any(ContractOffer.class)))
                .thenReturn(false);

        var result = negotiationManager.confirmed(token, negotiationId, contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getContractAgreement()).isNull();
    }

    @Test
    void testDeclined() {
        var negotiationId = createContractNegotiationConsumerOffered();
        var token = ClaimToken.Builder.newInstance().build();

        var result = negotiationManager.declined(token, negotiationId);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINED.code());
    }

    private String createContractNegotiationRequested() {
        var lastOffer = aContractOffer();

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("correlationId")
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(200)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .build();
        negotiation.addContractOffer(lastOffer);
        negotiations.put(negotiation.getId(), negotiation);
        return negotiation.getId();
    }

    private String createContractNegotiationConsumerOffered() {
        var lastOffer = aContractOffer();

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("correlationId")
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(600)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .build();
        negotiation.addContractOffer(lastOffer);
        negotiations.put(negotiation.getId(), negotiation);
        return negotiation.getId();
    }

    private ContractOffer aContractOffer() {
        return ContractOffer.Builder.newInstance().id("id").policy(Policy.Builder.newInstance().build()).build();
    }

}
