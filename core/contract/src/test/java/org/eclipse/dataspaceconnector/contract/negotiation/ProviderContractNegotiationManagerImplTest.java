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

class ProviderContractNegotiationManagerImplTest {

    private ContractValidationService validationService;
    private ProviderContractNegotiationManagerImpl negotiationManager;

    private final Map<String, ContractNegotiation> negotiations = new HashMap<>();

    private final String correlationId = "correlationId";

    @BeforeEach
    void setUp() throws Exception {
        negotiations.clear();

        // Mock contract negotiation store -> persist negotiations in map
        ContractNegotiationStore negotiationStore = EasyMock.createNiceMock(ContractNegotiationStore.class);
        negotiationStore.save(EasyMock.anyObject(ContractNegotiation.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            var negotiation = (ContractNegotiation) EasyMock.getCurrentArgument(0);
            if (ContractNegotiationStates.UNSAVED.code() == negotiation.getState()) {
                negotiation.transitionRequested();
            }
            negotiations.put(negotiation.getId(), negotiation);
            return null;
        });
        EasyMock.expect(negotiationStore.find(EasyMock.anyString())).andAnswer(() -> {
            var id = (String) EasyMock.getCurrentArgument(0);
            return negotiations.get(id);
        });
        EasyMock.expect(negotiationStore.findForCorrelationId(EasyMock.anyString())).andAnswer(() -> {
            var id = (String) EasyMock.getCurrentArgument(0);
            for (Map.Entry<String, ContractNegotiation> entry : negotiations.entrySet()) {
                if (entry.getValue().getCorrelationId().equals(id)) {
                    return entry.getValue();
                }
            }
            return null;
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

        negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .build();

        //TODO hand over store in start method, but run method should not be executed
        var negotiationStoreField = ProviderContractNegotiationManagerImpl.class.getDeclaredField("negotiationStore");
        negotiationStoreField.setAccessible(true);
        negotiationStoreField.set(negotiationManager, negotiationStore);
    }

    @Test
    void testRequestedConfirmOffer() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        EasyMock.expect(validationService.validate(token, contractOffer)).andReturn(Result.success(contractOffer));
        EasyMock.replay(validationService);

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
    }

    @Test
    void testRequestedDeclineOffer() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        EasyMock.expect(validationService.validate(token, contractOffer)).andReturn(Result.failure("error"));
        EasyMock.replay(validationService);

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
    }

    @Test
    @Disabled
    void testRequestedCounterOffer() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        var counterOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer, counterOffer);

        EasyMock.expect(validationService.validate(token, contractOffer)).andReturn(Result.success(null));
        EasyMock.replay(validationService);

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
    }

    @Test
    void testOfferReceivedInvalidId() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        var result = negotiationManager.offerReceived(token, "not a valid id", contractOffer, "hash");
        assertThat(result.failure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testOfferReceivedConfirmOffer() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(Result.success(contractOffer));
        EasyMock.replay(validationService);

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMING.code());
        assertThat(negotiation.getContractOffers()).hasSize(2);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer);

        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(Result.failure("error"));
        EasyMock.replay(validationService);

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

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
        var negotiationId = createContractNegotiationProviderOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        var counterOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(token, contractOffer, counterOffer);

        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.eq(contractOffer), EasyMock.anyObject(ContractOffer.class)))
                .andReturn(Result.success(null));
        EasyMock.replay(validationService);

        var result = negotiationManager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.PROVIDER_OFFERING.code());
        assertThat(negotiation.getContractOffers()).hasSize(3);
        assertThat(negotiation.getContractOffers().get(1)).isEqualTo(contractOffer);
        assertThat(negotiation.getContractOffers().get(2)).isEqualTo(counterOffer);
    }

    @Test
    void testConsumerApprovedInvalidId() {
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(token, contractAgreement);

        var result = negotiationManager.consumerApproved(token, "not a valid id", contractAgreement, "hash");
        assertThat(result.failure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConsumerApprovedConfirmAgreement() {
        var negotiationId = createContractNegotiationProviderOffered();

        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(token, contractAgreement);

        EasyMock.expect(validationService.validate(token, contractAgreement)).andReturn(true);
        EasyMock.replay(validationService);

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
        var token = (ClaimToken) EasyMock.createNiceMock(ClaimToken.class);
        EasyMock.replay(token);

        var result = negotiationManager.declined(token, correlationId);

        assertThat(result.succeeded()).isTrue();
        assertThat(negotiations).hasSize(1);
        var negotiation = negotiations.values().iterator().next();
        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINED.code());
    }

    private String createContractNegotiationProviderOffered() {
        var lastOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(lastOffer);

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

}
