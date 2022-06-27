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
 *       Microsoft Corporation - introduced Awaitility
 *
 */

package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class ContractNegotiationIntegrationTest extends AbstractContractNegotiationIntegrationTest {

    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);
    private final ContractNegotiationListener negotiationListenerMock = mock(ContractNegotiationListener.class);

    @Test
    void testNegotiation_initialOfferAccepted() {
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();
        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                any(ContractOffer.class))).thenReturn(true);

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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


        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    assertThat(consumerNegotiationId).isNotNull();
                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same offers and agreement stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 1);
                    assertThat(consumerNegotiation.getState()).isEqualTo(CONFIRMED.code());
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());
                    assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

                    // verify that the preConfirmed event has occurred twice - once for cons. once for prov
                    verify(negotiationListenerMock, times(2)).preConfirmed(any());

                    verify(validationService, atLeastOnce()).validate(token, offer);
                    verify(validationService, atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));
                });


        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_initialOfferDeclined() {
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();

        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));

        // Create and register listeners for provider and consumer

        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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

        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {

                    assertThat(consumerNegotiationId).isNotNull();
                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 1);
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // verify that the preConfirmed event has occurred twice - once for cons. once for prov
                    verify(negotiationListenerMock, times(2)).preDeclined(any());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();
                    verify(validationService, atLeastOnce()).validate(token, offer);
                });

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_agreementDeclined() {
        consumerNegotiationId = null;
        var offer = getContractOffer();

        when(validationService.validate(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                any(ContractOffer.class))).thenReturn(false);

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(offer)
                .protocol("protocol")
                .build();
        consumerManager.initiate(request);

        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    assertThat(consumerNegotiationId).isNotNull();
                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 1);
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();
                    // verify that the preConfirmed event has occurred twice - once for cons. once for prov
                    verify(negotiationListenerMock, times(2)).preDeclined(any());

                    verify(validationService, atLeastOnce()).validate(token, offer);
                    verify(validationService, atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));
                });

        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferAccepted() {
        consumerNegotiationId = null;
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(eq(token), any(ContractAgreement.class),
                eq(counterOffer))).thenReturn(true);

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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

        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {

                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same number of offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 2);

                    // Assert that initial offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

                    // Assert that counter offer is the same
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that same agreement is stored on both sides
                    assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
                    assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

                    verify(validationService, atLeastOnce()).validate(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));
                    verify(negotiationListenerMock, times(2)).preConfirmed(any());
                });
        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferDeclined() {
        consumerNegotiationId = null;

        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validate(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        // Create and register listeners for provider and consumer
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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
        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {

                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same number of offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 2);

                    // Assert that initial offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

                    // Assert that counter offer is the same
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();
                    verify(validationService, atLeastOnce()).validate(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(eq(token), any(ContractAgreement.class), any(ContractOffer.class));

                    verify(negotiationListenerMock, times(2)).preDeclined(any());
                });
        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferAccepted() {
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
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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
        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {

                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same number of offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 3);

                    // Assert that initial offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

                    // Assert that first counter offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(1)).isEqualTo(providerNegotiation.getContractOffers().get(1));

                    // Assert that second counter offer is the same
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that same agreement is stored on both sides
                    assertThat(consumerNegotiation.getContractAgreement()).isNotNull();
                    assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

                    verify(validationService, atLeastOnce()).validate(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);
                    verify(validationService, atLeastOnce()).validate(eq(token), any(ContractAgreement.class), eq(consumerCounterOffer));

                    verify(negotiationListenerMock, times(2)).preConfirmed(any());

                });
        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    @Disabled
    void testNegotiation_consumerCounterOfferDeclined() {
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
        providerObservable.registerListener(negotiationListenerMock);
        consumerObservable.registerListener(negotiationListenerMock);

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
        // Wait for negotiation to finish with time out at 15 seconds
        await().atMost(DEFAULT_TEST_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {

                    var consumerNegotiation = consumerStore.find(consumerNegotiationId);
                    var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);

                    // Assert that provider and consumer have the same number of offers stored
                    assertNegotiations(consumerNegotiation, providerNegotiation, 3);

                    // Assert that initial offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(providerNegotiation.getContractOffers().get(0));

                    // Assert that first counter offer is the same
                    assertThat(consumerNegotiation.getContractOffers().get(1)).isEqualTo(providerNegotiation.getContractOffers().get(1));

                    // Assert that second counter offer is the same
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();

                    verify(validationService, atLeastOnce()).validate(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);
                    verify(negotiationListenerMock, times(2)).preDeclined(any());
                });
        // Stop provider and consumer negotiation managers
        providerManager.stop();
        consumerManager.stop();
    }

    private void assertNegotiations(ContractNegotiation consumerNegotiation, ContractNegotiation providerNegotiation, int expectedSize) {
        assertThat(consumerNegotiation).isNotNull();
        assertThat(providerNegotiation).isNotNull();
        assertThat(consumerNegotiation.getContractOffers()).hasSize(expectedSize);
        assertThat(consumerNegotiation.getContractOffers().size()).isEqualTo(providerNegotiation.getContractOffers().size());
    }

}
