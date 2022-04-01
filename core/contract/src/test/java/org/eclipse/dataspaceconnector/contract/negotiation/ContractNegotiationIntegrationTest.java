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

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractNegotiationIntegrationTest extends AbstractContractNegotiationIntegrationTest {

    @Test
    void testNegotiation_initialOfferAccepted() throws Exception {
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();
        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                any(ContractOffer.class))).thenReturn(true);

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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
        assertThat(consumerNegotiation).isNotNull();
        assertThat(consumerNegotiation.getState()).isEqualTo(CONFIRMED.code());
        assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());
        assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());
        assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
        assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());
        verify(validationService, Mockito.atLeastOnce()).validate(token, offer);
        verify(validationService, Mockito.atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_initialOfferDeclined() throws Exception {
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();

        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));
    
        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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
        verify(validationService, Mockito.atLeastOnce()).validate(token, offer);

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_agreementDeclined() throws Exception {
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();

        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                any(ContractOffer.class))).thenReturn(false);
    
        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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
        verify(validationService, Mockito.atLeastOnce()).validate(token, offer);
        verify(validationService, Mockito.atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferAccepted() throws Exception {
        consumerNegotiationId = null;
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                eq(counterOffer))).thenReturn(true);

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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

        verify(validationService, Mockito.atLeastOnce()).validate(token, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, counterOffer, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferDeclined() throws Exception {
        consumerNegotiationId = null;

        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));
    
        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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
        verify(validationService, Mockito.atLeastOnce()).validate(token, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, counterOffer, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferAccepted() throws Exception {
        consumerNegotiationId = null;

        // Create an initial contract offer and two counter offers
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();
        ContractOffer consumerCounterOffer = getConsumerCounterOffer();

        // Mock validation of initial offer on provider side => counter offer
        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of counter offer on consumer side => counter offer
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of second counter offer on provider side => accept
        when(validationService.validate(token, consumerCounterOffer, counterOffer)).thenReturn(Result.success(null));

        // Mock validation of agreement on consumer side
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                eq(consumerCounterOffer))).thenReturn(true);
        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new ConfirmedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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

        verify(validationService, Mockito.atLeastOnce()).validate(token, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, counterOffer, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(eq(token), any(ContractAgreement.class), eq(consumerCounterOffer));

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferDeclined() throws Exception {
        consumerNegotiationId = null;

        // Create an initial contract offer and two counter offers
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();
        ContractOffer consumerCounterOffer = getConsumerCounterOffer();

        // Mock validation of initial offer on provider side => counter offer
        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of counter offer on consumer side => counter offer
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of second counter offer on provider side => decline
        when(validationService.validate(token, consumerCounterOffer, counterOffer)).thenReturn(Result.success(null));
        
        // Create and register listeners for provider and consumer
        providerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));
        consumerObservable.registerListener(new DeclinedContractNegotiationListener(countDownLatch));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

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

        verify(validationService, Mockito.atLeastOnce()).validate(token, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, counterOffer, initialOffer);
        verify(validationService, Mockito.atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

}
