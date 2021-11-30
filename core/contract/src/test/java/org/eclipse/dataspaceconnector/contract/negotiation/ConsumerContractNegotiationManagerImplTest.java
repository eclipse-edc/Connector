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

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerContractNegotiationManagerImplTest {

    private ContractValidationService validationService;
    private ConsumerContractNegotiationManagerImpl negotiationManager;

    private final Map<String, ContractNegotiation> negotiations = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        negotiations.clear();

        // Mock contract negotiation store -> persist negotiations in map
        ContractNegotiationStore negotiationStore = EasyMock.createNiceMock(ContractNegotiationStore.class);
        negotiationStore.save(EasyMock.anyObject(ContractNegotiation.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            var negotiation = (ContractNegotiation) EasyMock.getCurrentArgument(0);
            if (ContractNegotiationStates.UNSAVED.code() == negotiation.getState()) {
                negotiation.transitionRequesting();
            }
            negotiations.put(negotiation.getId(), negotiation);
            return null;
        });
        EasyMock.expect(negotiationStore.find(EasyMock.anyString())).andAnswer(() -> {
            var id = (String) EasyMock.getCurrentArgument(0);
            return negotiations.get(id);
        });
        EasyMock.replay(negotiationStore);

        // Create contract validation service mock, method mocking has to be done in test methods
        validationService = EasyMock.createNiceMock(ContractValidationService.class);

        // Create dispatcher registry mock in order to create manager, not used in unit tests
        RemoteMessageDispatcherRegistry dispatcherRegistry = EasyMock.createNiceMock(RemoteMessageDispatcherRegistry.class);
        EasyMock.replay(dispatcherRegistry);

        // Create monitor mock
        Monitor monitor = EasyMock.createNiceMock(Monitor.class);
        EasyMock.replay(monitor);

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
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(contractOffer);

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .build();

        NegotiationResponse result = negotiationManager.initiate(request);

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
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
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        NegotiationResponse result = negotiationManager.offerReceived(token, "not a valid id", contractOffer, "hash");
        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.FATAL_ERROR);
    }

    @Test
    void testOfferReceivedConfirmOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        // Create result for passed validation
        var validationResult = new OfferValidationResult(contractOffer, null);
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(validationResult);
        EasyMock.replay(validationService);

        NegotiationResponse result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONSUMER_APPROVING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        // Create result for failed validation
        var validationResult = OfferValidationResult.INVALID;
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(validationResult);
        EasyMock.replay(validationService);

        NegotiationResponse result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
    }

    @Test
    void testOfferReceivedCounterOffer() {
        var negotiationId = createContractNegotiationRequested();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        var counterOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer, counterOffer);

        // Create result containing counter offer
        var validationResult = new OfferValidationResult(null, counterOffer);
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(validationResult);
        EasyMock.replay(validationService);

        NegotiationResponse result = negotiationManager.offerReceived(token, negotiationId, contractOffer, "hash");

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONSUMER_OFFERING.code());
        assertThat(negotiation.getContractOffers()).hasSize(3);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        assertThat(negotiation.getContractOffers().get(2)).isEqualTo(counterOffer);
    }

    @Test
    void testConfirmedInvalidId() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(token, contractAgreement);

        NegotiationResponse result = negotiationManager.confirmed(token, "not a valid id", contractAgreement, "hash");
        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.FATAL_ERROR);
    }

    @Test
    void testConfirmedConfirmAgreement() {
        var negotiationId = createContractNegotiationConsumerOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(token, contractAgreement);

        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractAgreement), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(true);
        EasyMock.replay(validationService);

        NegotiationResponse result = negotiationManager.confirmed(token, negotiationId, contractAgreement, "hash");

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMED.code());
        assertThat(negotiation.getContractAgreement()).isEqualTo(contractAgreement);
    }

    @Test
    void testConfirmedDeclineAgreement() {
        var negotiationId = createContractNegotiationConsumerOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(token, contractAgreement);

        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractAgreement), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(false);
        EasyMock.replay(validationService);

        NegotiationResponse result = negotiationManager.confirmed(token, negotiationId, contractAgreement, "hash");

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getContractAgreement()).isNull();
    }

    @Test
    void testDeclined() {
        var negotiationId = createContractNegotiationConsumerOffered();
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        EasyMock.replay(token);

        NegotiationResponse result = negotiationManager.declined(token, negotiationId);

        assertThat(result.getStatus()).isEqualTo(NegotiationResponse.Status.OK);
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINED.code());
    }

    private String createContractNegotiationRequested() {
        var lastOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(lastOffer);

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
        var lastOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(lastOffer);

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

}
