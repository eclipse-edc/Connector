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
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ContractNegotiationIntegrationTest extends AbstractContractNegotiationIntegrationTest {

    @Test
    void testNegotiation_initialOfferAccepted() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create a contract offer
        ContractOffer offer = getContractOffer();

        // Mock validation service methods
        var validationResult = new OfferValidationResult(offer);
        EasyMock.expect(validationService.validate(token, offer)).andReturn(validationResult);
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.anyObject(ContractAgreement.class),
                EasyMock.anyObject(ContractOffer.class))).andReturn(true);
        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(offer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same offers and agreement stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());
        assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
        assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_initialOfferDeclined() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create a contract offer
        ContractOffer offer = getContractOffer();

        // Mock validation service methods
        var validationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, offer)).andReturn(validationResult);
        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(offer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that no agreement has been stored on either side
        assertThat(consumerNegotiation.getContractAgreement()).isNull();
        assertThat(providerNegotiation.getContractAgreement()).isNull();

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_agreementDeclined() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create a contract offer
        ContractOffer offer = getContractOffer();

        // Mock validation service methods
        var validationResult = new OfferValidationResult(offer);
        EasyMock.expect(validationService.validate(token, offer)).andReturn(validationResult);
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.anyObject(ContractAgreement.class),
                EasyMock.anyObject(ContractOffer.class))).andReturn(false);
        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(offer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that no agreement has been stored on either side
        assertThat(consumerNegotiation.getContractAgreement()).isNull();
        assertThat(providerNegotiation.getContractAgreement()).isNull();

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferAccepted() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create an initial contract offer and a counter offer
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        // Mock validation service methods
        var providerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, initialOffer)).andReturn(providerValidationResult);
        var consumerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, counterOffer, initialOffer)).andReturn(consumerValidationResult);
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.anyObject(ContractAgreement.class),
                EasyMock.eq(counterOffer))).andReturn(true);
        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(initialOffer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same number of offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(2);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());

        // Assert that initial offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

        // Assert that counter offer is the same
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that same agreement is stored on both sides
        assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
        assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferDeclined() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create an initial contract offer and a counter offer
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        // Mock validation service methods
        var providerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, initialOffer)).andReturn(providerValidationResult);
        var consumerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, counterOffer, initialOffer)).andReturn(consumerValidationResult);
        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(initialOffer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same number of offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(2);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());

        // Assert that initial offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

        // Assert that counter offer is the same
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that no agreement has been stored on either side
        assertThat(consumerNegotiation.getContractAgreement()).isNull();
        assertThat(providerNegotiation.getContractAgreement()).isNull();

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferAccepted() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create an initial contract offer and two counter offers
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();
        ContractOffer consumerCounterOffer = getConsumerCounterOffer();

        // Mock validation of initial offer on provider side => counter offer
        var providerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, initialOffer)).andReturn(providerValidationResult);

        //Mock validation of counter offer on consumer side => counter offer
        var consumerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, counterOffer, initialOffer)).andReturn(consumerValidationResult);

        //Mock validation of second counter offer on provider side => accept
        var providerValidationResult2 = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, consumerCounterOffer, counterOffer)).andReturn(providerValidationResult2);

        // Mock validation of agreement on consumer side
        EasyMock.expect(validationService.validate(EasyMock.eq(token), EasyMock.anyObject(ContractAgreement.class),
                EasyMock.eq(consumerCounterOffer))).andReturn(true);

        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.CONFIRMED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(initialOffer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same number of offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(3);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());

        // Assert that initial offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

        // Assert that first counter offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(1)).isEqualTo(providerNegotiation.getContractOffers().get(1));

        // Assert that second counter offer is the same
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that same agreement is stored on both sides
        assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
        assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferDeclined() throws Exception {
        // Reset the consumer negotiation id
        consumerNegotiationId = null;

        // Create an initial contract offer and two counter offers
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();
        ContractOffer consumerCounterOffer = getConsumerCounterOffer();

        // Mock validation of initial offer on provider side => counter offer
        var providerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, initialOffer)).andReturn(providerValidationResult);

        //Mock validation of counter offer on consumer side => counter offer
        var consumerValidationResult = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, counterOffer, initialOffer)).andReturn(consumerValidationResult);

        //Mock validation of second counter offer on provider side => decline
        var providerValidationResult2 = new OfferValidationResult(null);
        EasyMock.expect(validationService.validate(token, consumerCounterOffer, counterOffer)).andReturn(providerValidationResult2);

        EasyMock.replay(validationService);

        // Create signaling stores for provider and consumer
        providerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);
        consumerStore = new SignalingInMemoryContractNegotiationStore(countDownLatch, ContractNegotiationStates.DECLINED);

        // Start provider and consumer negotiation managers
        providerManager.start(providerStore);
        consumerManager.start(consumerStore);

        // Create an initial request and trigger consumer manager
        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(initialOffer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        // Wait for negotiation to finish with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        var consumerNegotiation = consumerStore.find(consumerNegotiationId);
        var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

        // Assert that provider and consumer have the same number of offers stored
        assertThat(consumerNegotiation.getContractOffers()).hasSize(3);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());

        // Assert that initial offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

        // Assert that first counter offer is the same
        assertThat(consumerNegotiation.getContractOffers().get(1)).isEqualTo(providerNegotiation.getContractOffers().get(1));

        // Assert that second counter offer is the same
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

        // Assert that no agreement has been stored on either side
        assertThat(consumerNegotiation.getContractAgreement()).isNull();
        assertThat(providerNegotiation.getContractAgreement()).isNull();

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

}
