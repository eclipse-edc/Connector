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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.service.contractnegotiation.ContractNegotiationProtocolServiceImpl;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
    private static final String CONSUMER_ID = "consumer";
    private static final String PROVIDER_ID = "provider";

    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);
    private final InMemoryContractNegotiationStore providerStore = new InMemoryContractNegotiationStore();
    private final InMemoryContractNegotiationStore consumerStore = new InMemoryContractNegotiationStore();
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final RemoteMessageDispatcherRegistry providerDispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final RemoteMessageDispatcherRegistry consumerDispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    protected ClaimToken token = ClaimToken.Builder.newInstance().build();
    private final ProtocolWebhook protocolWebhook = () -> "http://dummy";
    private String consumerNegotiationId;

    private ProviderContractNegotiationManagerImpl providerManager;
    private ConsumerContractNegotiationManagerImpl consumerManager;
    private ContractNegotiationProtocolService consumerService;
    private ContractNegotiationProtocolService providerService;

    @BeforeEach
    void init() {
        var monitor = new ConsoleMonitor();

        CommandQueue<ContractNegotiationCommand> queue = mock();
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        CommandRunner<ContractNegotiationCommand> runner = mock();

        providerManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(PROVIDER_ID)
                .dispatcherRegistry(providerDispatcherRegistry)
                .monitor(monitor)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(new ContractNegotiationObservableImpl())
                .store(providerStore)
                .policyStore(mock(PolicyDefinitionStore.class))
                .protocolWebhook(protocolWebhook)
                .build();

        consumerManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(CONSUMER_ID)
                .dispatcherRegistry(consumerDispatcherRegistry)
                .monitor(monitor).waitStrategy(() -> 1000)
                .commandQueue(queue).commandRunner(runner)
                .observable(new ContractNegotiationObservableImpl())
                .store(consumerStore)
                .policyStore(mock(PolicyDefinitionStore.class))
                .protocolWebhook(protocolWebhook)
                .build();

        consumerService = new ContractNegotiationProtocolServiceImpl(consumerStore, new NoopTransactionContext(), validationService, new ContractNegotiationObservableImpl(), monitor, mock(Telemetry.class));
        providerService = new ContractNegotiationProtocolServiceImpl(providerStore, new NoopTransactionContext(), validationService, new ContractNegotiationObservableImpl(), monitor, mock(Telemetry.class));
    }

    @AfterEach
    void tearDown() {
        providerManager.stop();
        consumerManager.stop();
    }

    @Test
    void testNegotiation_initialOfferAccepted() {
        when(consumerDispatcherRegistry.dispatch(any(), isA(ContractRequestMessage.class))).then(onConsumerSentOfferRequest());
        when(providerDispatcherRegistry.dispatch(any(), isA(ContractAgreementMessage.class))).then(onProviderSentAgreementRequest());
        when(consumerDispatcherRegistry.dispatch(any(), isA(ContractAgreementVerificationMessage.class))).then(onConsumerSentAgreementVerification());
        when(providerDispatcherRegistry.dispatch(any(), isA(ContractNegotiationEventMessage.class))).then(onProviderSentNegotiationEventMessage());
        consumerNegotiationId = "consumerNegotiationId";
        var offer = getContractOffer();
        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.success(new ValidatedConsumerOffer(CONSUMER_ID, offer)));
        when(validationService.validateConfirmed(eq(token), any(ContractAgreement.class), any(ContractOffer.class))).thenReturn(Result.success());
        when(validationService.validateRequest(eq(token), any(ContractNegotiation.class))).thenReturn(Result.success());

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var requestData = ContractRequestData.Builder.newInstance().connectorId(PROVIDER_ID).counterPartyAddress("callbackAddress").contractOffer(offer).protocol("protocol").build();

        var request = ContractRequest.Builder.newInstance().callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("local://test").build())).requestData(requestData).build();

        consumerManager.initiate(request);

        await().atMost(DEFAULT_TEST_TIMEOUT).pollInterval(DEFAULT_POLL_INTERVAL).untilAsserted(() -> {
            assertThat(consumerNegotiationId).isNotNull();
            var consumerNegotiation = consumerStore.findById(consumerNegotiationId);
            var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);
            assertThat(consumerNegotiation).isNotNull();
            assertThat(providerNegotiation).isNotNull();

            // Assert that the consumer has the callbacks
            assertThat(consumerNegotiation.getCallbackAddresses()).hasSize(1);
            assertThat(providerNegotiation.getCallbackAddresses()).hasSize(0);

            // Assert that provider and consumer have the same offers and agreement stored
            assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
            assertThat(providerNegotiation.getContractOffers()).hasSize(1);
            assertThat(consumerNegotiation.getContractOffers().get(0)).isEqualTo(offer);
            assertThat(consumerNegotiation.getState()).isEqualTo(ContractNegotiationStates.FINALIZED.code());
            assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());
            assertThat(consumerNegotiation.getContractAgreement()).isEqualTo(providerNegotiation.getContractAgreement());

            verify(validationService, atLeastOnce()).validateInitialOffer(token, offer);
            verify(validationService, atLeastOnce()).validateConfirmed(eq(token), any(ContractAgreement.class), any(ContractOffer.class));
        });
    }

    @Test
    void testNegotiation_initialOfferDeclined() {
        when(providerDispatcherRegistry.dispatch(any(), isA(ContractNegotiationTerminationMessage.class))).then(onProviderSentRejection());
        when(consumerDispatcherRegistry.dispatch(any(), isA(ContractRequestMessage.class))).then(onConsumerSentOfferRequest());
        consumerNegotiationId = null;
        var offer = getContractOffer();

        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.failure("must be declined"));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var requestData = ContractRequestData.Builder.newInstance().connectorId("connectorId").counterPartyAddress("callbackAddress").contractOffer(offer).protocol("protocol").build();

        var request = ContractRequest.Builder.newInstance().requestData(requestData).build();

        consumerManager.initiate(request);

        await().atMost(DEFAULT_TEST_TIMEOUT).pollInterval(DEFAULT_POLL_INTERVAL).untilAsserted(() -> verify(validationService, atLeastOnce()).validateInitialOffer(token, offer));
    }

    @Test
    void testNegotiation_agreementDeclined() {
        when(providerDispatcherRegistry.dispatch(any(), isA(ContractAgreementMessage.class))).then(onProviderSentAgreementRequest());
        when(consumerDispatcherRegistry.dispatch(any(), isA(ContractRequestMessage.class))).then(onConsumerSentOfferRequest());
        when(consumerDispatcherRegistry.dispatch(any(), isA(ContractNegotiationTerminationMessage.class))).then(onConsumerSentRejection());
        consumerNegotiationId = null;
        var offer = getContractOffer();

        when(validationService.validateInitialOffer(token, offer)).thenReturn(Result.success(new ValidatedConsumerOffer(CONSUMER_ID, offer)));
        when(validationService.validateConfirmed(eq(token), any(ContractAgreement.class), any(ContractOffer.class))).thenReturn(Result.failure("error"));

        // Start provider and consumer negotiation managers
        providerManager.start();
        consumerManager.start();

        // Create an initial request and trigger consumer manager
        var requestData = ContractRequestData.Builder.newInstance().connectorId("connectorId").counterPartyAddress("callbackAddress").contractOffer(offer).protocol("protocol").build();

        var request = ContractRequest.Builder.newInstance().requestData(requestData).callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("local://test").build())).build();
        consumerManager.initiate(request);

        await().atMost(DEFAULT_TEST_TIMEOUT).pollInterval(DEFAULT_POLL_INTERVAL).untilAsserted(() -> {
            assertThat(consumerNegotiationId).isNotNull();
            var consumerNegotiation = consumerStore.findById(consumerNegotiationId);
            var providerNegotiation = providerStore.findForCorrelationId(consumerNegotiationId);
            assertThat(consumerNegotiation).isNotNull();
            assertThat(providerNegotiation).isNotNull();

            // Assert that the consumer has the callbacks
            assertThat(consumerNegotiation.getCallbackAddresses()).hasSize(1);
            assertThat(providerNegotiation.getCallbackAddresses()).hasSize(0);

            // Assert that provider and consumer have the same offers stored
            assertThat(consumerNegotiation.getContractOffers()).hasSize(1);
            assertThat(providerNegotiation.getContractOffers()).hasSize(1);
            assertThat(consumerNegotiation.getLastContractOffer()).isEqualTo(providerNegotiation.getLastContractOffer());

            // Assert that no agreement has been stored on either side
            assertThat(consumerNegotiation.getContractAgreement()).isNull();
            assertThat(providerNegotiation.getContractAgreement()).isNull();

            verify(validationService, atLeastOnce()).validateInitialOffer(token, offer);
            verify(validationService, atLeastOnce()).validateConfirmed(eq(token), any(ContractAgreement.class), any(ContractOffer.class));
        });
    }

    private Answer<Object> onConsumerSentOfferRequest() {
        return i -> {
            ContractRequestMessage request = i.getArgument(1);
            consumerNegotiationId = request.getProcessId();
            var result = providerService.notifyRequested(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onConsumerSentRejection() {
        return i -> {
            ContractNegotiationTerminationMessage request = i.getArgument(1);
            var result = providerService.notifyTerminated(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onProviderSentAgreementRequest() {
        return i -> {
            ContractAgreementMessage request = i.getArgument(1);
            var result = consumerService.notifyAgreed(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onProviderSentNegotiationEventMessage() {
        return i -> {
            ContractNegotiationEventMessage request = i.getArgument(1);
            var result = consumerService.notifyFinalized(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onConsumerSentAgreementVerification() {
        return i -> {
            ContractAgreementVerificationMessage request = i.getArgument(1);
            var result = providerService.notifyVerified(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private Answer<Object> onProviderSentRejection() {
        return i -> {
            ContractNegotiationTerminationMessage request = i.getArgument(1);
            var result = consumerService.notifyTerminated(request, token);
            return toFuture(result);
        };
    }

    @NotNull
    private CompletableFuture<?> toFuture(ServiceResult<ContractNegotiation> result) {
        if (result.succeeded()) {
            return completedFuture(StatusResult.success("Success!"));
        } else {
            return failedFuture(new Exception("Negotiation failed."));
        }
    }

    /**
     * Creates the initial contract offer.
     *
     * @return the contract offer.
     */
    private ContractOffer getContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ContractId.create("1", "test-asset-id").toString())
                .providerId("provider")
                .assetId(randomUUID().toString())
                .policy(Policy.Builder.newInstance()
                        .type(PolicyType.CONTRACT)
                        .assigner("assigner")
                        .assignee("assignee")
                        .duty(Duty.Builder.newInstance()
                                .action(Action.Builder.newInstance().type("USE").build())
                                .build())
                        .build())
                .build();
    }

}
