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

import org.eclipse.dataspaceconnector.common.statemachine.retry.SendRetryManager;
import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.dataspaceconnector.core.defaults.negotiationstore.InMemoryContractNegotiationStore;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Setup for the contract negotiation integration test.
 */
public abstract class AbstractContractNegotiationIntegrationTest {

    protected final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    protected ProviderContractNegotiationManagerImpl providerManager;
    protected ConsumerContractNegotiationManagerImpl consumerManager;
    protected ContractNegotiationObservable providerObservable = new ContractNegotiationObservableImpl();
    protected ContractNegotiationObservable consumerObservable = new ContractNegotiationObservableImpl();
    protected InMemoryContractNegotiationStore providerStore;
    protected InMemoryContractNegotiationStore consumerStore;
    protected ContractValidationService validationService;
    protected SendRetryManager<StatefulEntity> sendRetryManager = mock(SendRetryManager.class);
    protected String consumerNegotiationId;

    protected ClaimToken token;

    /**
     * Prepares the test setup
     */
    @BeforeEach
    void setUp() {
        // Create contract validation service mock, method mocking has to be done in test methods
        validationService = mock(ContractValidationService.class);

        // Create a monitor that logs to the console
        Monitor monitor = new ConsoleMonitor();

        // Create CommandQueue mock
        CommandQueue<ContractNegotiationCommand> queue = (CommandQueue<ContractNegotiationCommand>) mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        // Create CommandRunner mock
        CommandRunner<ContractNegotiationCommand> runner = (CommandRunner<ContractNegotiationCommand>) mock(CommandRunner.class);

        providerStore = new InMemoryContractNegotiationStore();
        consumerStore = new InMemoryContractNegotiationStore();

        providerManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(new FakeProviderDispatcherRegistry())
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(providerObservable)
                .store(providerStore)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();

        consumerManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(new FakeConsumerDispatcherRegistry())
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(consumerObservable)
                .store(consumerStore)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();

    }

    /**
     * Creates the initial contract offer.
     *
     * @return the contract offer.
     */
    protected ContractOffer getContractOffer() {
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
    protected ContractOffer getCounterOffer() {
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
    protected ContractOffer getConsumerCounterOffer() {
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

    /**
     * Implementation of the RemoteMessageDispatcherRegistry for the provider that delegates the requests to the
     * consumer negotiation manager directly.
     */
    protected class FakeProviderDispatcherRegistry implements RemoteMessageDispatcherRegistry {

        @Override
        public void register(RemoteMessageDispatcher dispatcher) {
            // Not needed for test
        }

        @Override
        public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
            return (CompletableFuture<T>) send(message);
        }

        public CompletableFuture<Object> send(RemoteMessage message) {
            StatusResult<ContractNegotiation> result;
            if (message instanceof ContractOfferRequest) {
                var request = (ContractOfferRequest) message;
                result = consumerManager.offerReceived(token, request.getCorrelationId(), request.getContractOffer(), "hash");
            } else if (message instanceof ContractAgreementRequest) {
                var request = (ContractAgreementRequest) message;
                result = consumerManager.confirmed(token, request.getCorrelationId(), request.getContractAgreement(), request.getPolicy());
            } else if (message instanceof ContractRejection) {
                var request = (ContractRejection) message;
                result = consumerManager.declined(token, request.getCorrelationId());
            } else {
                throw new IllegalArgumentException("Unknown message type.");
            }

            CompletableFuture<Object> future = new CompletableFuture<>();
            if (result.succeeded()) {
                future.complete("Success!");
            } else {
                future.completeExceptionally(new Exception("Negotiation failed."));
            }

            return future;
        }
    }

    /**
     * Implementation of the RemoteMessageDispatcherRegistry for the consumer that delegates the requests to the
     * provider negotiation manager directly.
     */
    protected class FakeConsumerDispatcherRegistry implements RemoteMessageDispatcherRegistry {

        @Override
        public void register(RemoteMessageDispatcher dispatcher) {
            // Not needed for test
        }

        @Override
        public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
            return (CompletableFuture<T>) send(message);
        }

        public CompletableFuture<Object> send(RemoteMessage message) {
            StatusResult<ContractNegotiation> result;
            if (message instanceof ContractOfferRequest) {
                var request = (ContractOfferRequest) message;
                consumerNegotiationId = request.getCorrelationId();
                result = providerManager.offerReceived(token, request.getCorrelationId(), request.getContractOffer(), "hash");
                if (result.fatalError()) {
                    result = providerManager.requested(token, request);
                }
            } else if (message instanceof ContractAgreementRequest) {
                var request = (ContractAgreementRequest) message;
                result = providerManager.consumerApproved(token, request.getCorrelationId(), request.getContractAgreement(), "hash");
            } else if (message instanceof ContractRejection) {
                var request = (ContractRejection) message;
                result = providerManager.declined(token, request.getCorrelationId());
            } else {
                throw new IllegalArgumentException("Unknown message type.");
            }

            CompletableFuture<Object> future = new CompletableFuture<>();
            if (result.succeeded()) {
                future.complete("Success!");
            } else {
                future.completeExceptionally(new Exception("Negotiation failed."));
            }

            return future;
        }
    }

}
