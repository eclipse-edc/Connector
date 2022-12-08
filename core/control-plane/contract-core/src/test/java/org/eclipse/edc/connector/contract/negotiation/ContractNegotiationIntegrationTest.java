/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.connector.contract.negotiation;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRejection;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class ContractNegotiationIntegrationTest {

    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);
    private final InMemoryContractNegotiationStore providerStore = new InMemoryContractNegotiationStore();
    private final InMemoryContractNegotiationStore consumerStore = new InMemoryContractNegotiationStore();
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final RemoteMessageDispatcherRegistry providerDispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final RemoteMessageDispatcherRegistry consumerDispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    protected ClaimToken token = ClaimToken.Builder.newInstance().build();
    private String consumerNegotiationId;

    private ProviderContractNegotiationManagerImpl providerManager;
    private ConsumerContractNegotiationManagerImpl consumerManager;

    @NotNull
    private static CompletableFuture<?> toFuture(StatusResult<ContractNegotiation> result) {
        if (result.succeeded()) {
            return completedFuture("Success!");
        } else {
            return failedFuture(new Exception("Negotiation failed."));
        }
    }

    @BeforeEach
    void init() {
        var monitor = new ConsoleMonitor();

        CommandQueue<ContractNegotiationCommand> queue = (CommandQueue<ContractNegotiationCommand>) mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        CommandRunner<ContractNegotiationCommand> runner = (CommandRunner<ContractNegotiationCommand>) mock(CommandRunner.class);

        var sendRetryManager = mock(SendRetryManager.class);

        providerManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(providerDispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(new ContractNegotiationObservableImpl())
                .store(providerStore)
                .policyStore(mock(PolicyDefinitionStore.class))
                .sendRetryManager(sendRetryManager)
                .build();

        consumerManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(consumerDispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(new ContractNegotiationObservableImpl())
                .store(consumerStore)
                .policyStore(mock(PolicyDefinitionStore.class))
                .sendRetryManager(sendRetryManager)
                .build();
    }

    @AfterEach
    void tearDown() {
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_initialOfferAccepted() {
        when(providerDispatcherRegistry.send(any(), isA(ContractAgreementRequest.class), any())).then(onProviderSentAgreementRequest());
        when(consumerDispatcherRegistry.send(any(), isA(ContractOfferRequest.class), any())).then(onConsumerSentOfferRequest());
        consumerNegotiationId = "consumerNegotiationId";
        var offer = getContractOffer();
        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class)))
                .thenReturn(Result.success());

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
                    assertThat(consumerNegotiation).isNotNull();
                    assertThat(providerNegotiation).isNotNull();

                    // Assert that provider and consumer have the same offers and agreement stored
                    assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
                    assertThat(providerNegotiation.getContractOffers()).hasSize(2);
                    assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(offer);
                    assertThat(consumerNegotiation.getState()).isEqualTo(CONFIRMED.code());
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());
                    assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());


                    verify(validationService, atLeastOnce()).validateInitialOffer(token, offer);
                    verify(validationService, atLeastOnce()).validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class));
                });
    }

    @Test
    void testNegotiation_initialOfferDeclined() {
        when(consumerDispatcherRegistry.send(any(), isA(ContractOfferRequest.class), any())).then(onConsumerSentOfferRequest());
        consumerNegotiationId = null;
        ContractOffer offer = getContractOffer();

        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.success(offer));

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
                    assertThat(consumerNegotiation).isNotNull();
                    assertThat(providerNegotiation).isNotNull();

                    // Assert that provider and consumer have the same offers stored
                    assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
                    assertThat(providerNegotiation.getContractOffers()).hasSize(2);
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();
                    verify(validationService, atLeastOnce()).validateInitialOffer(token, offer);
                });
    }

    @Test
    void testNegotiation_agreementDeclined() {
        when(providerDispatcherRegistry.send(any(), isA(ContractAgreementRequest.class), any())).then(onProviderSentAgreementRequest());
        when(consumerDispatcherRegistry.send(any(), isA(ContractOfferRequest.class), any())).then(onConsumerSentOfferRequest());
        when(consumerDispatcherRegistry.send(any(), isA(ContractRejection.class), any())).then(onConsumerSentRejection());
        consumerNegotiationId = null;
        var offer = getContractOffer();

        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.success(offer));
        when(validationService.validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class)))
                .thenReturn(Result.failure("error"));

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
                    assertThat(consumerNegotiation).isNotNull();
                    assertThat(providerNegotiation).isNotNull();

                    // Assert that provider and consumer have the same offers stored
                    assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
                    assertThat(providerNegotiation.getContractOffers()).hasSize(2);
                    assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

                    // Assert that no agreement has been stored on either side
                    assertThat(consumerNegotiation.getContractAgreement()).isNull();
                    assertThat(providerNegotiation.getContractAgreement()).isNull();

                    verify(validationService, atLeastOnce()).validateInitialOffer(token, offer);
                    verify(validationService, atLeastOnce()).validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class));
                });
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferAccepted() {
        consumerNegotiationId = null;
        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validateInitialOffer(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validateConfirmed(any(ContractAgreement.class), eq(counterOffer)))
                .thenReturn(Result.success());

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var request = ContractOfferRequest.Builder.newInstance()
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

                    verify(validationService, atLeastOnce()).validateInitialOffer(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class));
                });
    }

    @Test
    @Disabled
    void testNegotiation_counterOfferDeclined() {
        consumerNegotiationId = null;

        ContractOffer initialOffer = getContractOffer();
        ContractOffer counterOffer = getCounterOffer();

        when(validationService.validateInitialOffer(token, initialOffer)).thenReturn(Result.success(null));
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var request = ContractOfferRequest.Builder.newInstance()
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
                    verify(validationService, atLeastOnce()).validateInitialOffer(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validateConfirmed(any(ContractAgreement.class), any(ContractOffer.class));
                });
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
        when(validationService.validateInitialOffer(token, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of counter offer on consumer side => counter offer
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of second counter offer on provider side => accept
        when(validationService.validate(token, consumerCounterOffer, counterOffer)).thenReturn(Result.success(null));

        // Mock validation of agreement on consumer side
        when(validationService.validateConfirmed(any(ContractAgreement.class), eq(consumerCounterOffer)))
                .thenReturn(Result.success());

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var request = ContractOfferRequest.Builder.newInstance()
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

                    verify(validationService, atLeastOnce()).validateInitialOffer(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);
                    verify(validationService, atLeastOnce()).validateConfirmed(any(ContractAgreement.class), eq(consumerCounterOffer));
                });
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
        when(validationService.validateInitialOffer(token, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of counter offer on consumer side => counter offer
        when(validationService.validate(token, counterOffer, initialOffer)).thenReturn(Result.success(null));

        //Mock validation of second counter offer on provider side => decline
        when(validationService.validate(token, consumerCounterOffer, counterOffer)).thenReturn(Result.success(null));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var request = ContractOfferRequest.Builder.newInstance()
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

                    verify(validationService, atLeastOnce()).validateInitialOffer(token, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, counterOffer, initialOffer);
                    verify(validationService, atLeastOnce()).validate(token, consumerCounterOffer, counterOffer);
                });
    }

    private Answer<Object> onConsumerSentOfferRequest() {
        return i -> {
            ContractOfferRequest request = i.getArgument(1);
            consumerNegotiationId = request.getCorrelationId();
            var result = providerManager.offerReceived(token, request.getCorrelationId(), request.getContractOffer(), "hash");
            if (result.fatalError()) {
                result = providerManager.requested(token, request);
            }
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onConsumerSentRejection() {
        return i -> {
            ContractRejection request = i.getArgument(1);
            var result = providerManager.declined(token, request.getCorrelationId());
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onProviderSentAgreementRequest() {
        return i -> {
            ContractAgreementRequest request = i.getArgument(1);
            var result = consumerManager.confirmed(token, request.getCorrelationId(), request.getContractAgreement(), request.getPolicy());
            return toFuture(result);
        };
    }

    private void assertNegotiations(ContractNegotiation consumerNegotiation, ContractNegotiation providerNegotiation, int expectedSize) {
        assertThat(consumerNegotiation).isNotNull();
        assertThat(providerNegotiation).isNotNull();
        assertThat(consumerNegotiation.getContractOffers()).hasSize(expectedSize);
        assertThat(consumerNegotiation.getContractOffers()).hasSameSizeAs(providerNegotiation.getContractOffers());
    }

    /**
     * Creates the initial contract offer.
     *
     * @return the contract offer.
     */
    private ContractOffer getContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId("1"))
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(1))
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .asset(Asset.Builder.newInstance().build())
                .policy(Policy.Builder.newInstance()
                        .type(PolicyType.CONTRACT)
                        .assigner("assigner")
                        .assignee("assignee")
                        .duty(Duty.Builder.newInstance()
                                .action(Action.Builder.newInstance()
                                        .type("USE")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    /**
     * Creates the first counter offer.
     *
     * @return the contract offer.
     */
    private ContractOffer getCounterOffer() {
        return ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(2))
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .policy(Policy.Builder.newInstance()
                        .type(PolicyType.CONTRACT)
                        .assigner("assigner")
                        .assignee("assignee")
                        .duty(Duty.Builder.newInstance()
                                .action(Action.Builder.newInstance()
                                        .type("USE")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    /**
     * Creates the second counter offer.
     *
     * @return the contract offer.
     */
    private ContractOffer getConsumerCounterOffer() {
        return ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(3))
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .policy(Policy.Builder.newInstance()
                        .type(PolicyType.CONTRACT)
                        .assigner("assigner")
                        .assignee("assignee")
                        .duty(Duty.Builder.newInstance()
                                .action(Action.Builder.newInstance()
                                        .type("USE")
                                        .build())
                                .build())
                        .build())
                .build();
    }

}
