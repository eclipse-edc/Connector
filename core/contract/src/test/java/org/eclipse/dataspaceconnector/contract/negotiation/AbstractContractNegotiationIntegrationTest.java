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

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.dataspaceconnector.negotiation.store.memory.InMemoryContractNegotiationStore;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
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
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Setup for the contract negotiation integration test.
 */
public abstract class AbstractContractNegotiationIntegrationTest {

    protected ProviderContractNegotiationManagerImpl providerManager;
    protected ConsumerContractNegotiationManagerImpl consumerManager;

    protected ContractNegotiationObservable providerObservable = new ContractNegotiationObservableImpl();
    protected ContractNegotiationObservable consumerObservable = new ContractNegotiationObservableImpl();

    protected InMemoryContractNegotiationStore providerStore;
    protected InMemoryContractNegotiationStore consumerStore;

    protected ContractValidationService validationService;

    protected String consumerNegotiationId;

    protected ClaimToken token;

    protected CountDownLatch countDownLatch;

    /**
     * Prepares the test setup
     */
    @BeforeEach
    void setUp() {
        // Create contract validation service mock, method mocking has to be done in test methods
        validationService = mock(ContractValidationService.class);

        // Create a monitor that logs to the console
        Monitor monitor = new FakeConsoleMonitor();
    
        // Create CommandQueue mock
        CommandQueue<ContractNegotiationCommand> queue = (CommandQueue<ContractNegotiationCommand>) mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());
    
        // Create CommandRunner mock
        CommandRunner<ContractNegotiationCommand> runner = (CommandRunner<ContractNegotiationCommand>) mock(CommandRunner.class);

        // Create the provider contract negotiation manager
        providerManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(new FakeProviderDispatcherRegistry())
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(providerObservable)
                .build();
        providerStore = new InMemoryContractNegotiationStore();

        // Create the consumer contract negotiation manager
        consumerManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(new FakeConsumerDispatcherRegistry())
                .monitor(monitor)
                .validationService(validationService)
                .waitStrategy(() -> 1000)
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(consumerObservable)
                .build();
        consumerStore = new InMemoryContractNegotiationStore();
        
        countDownLatch = new CountDownLatch(2);
    }
    
    /**
     * Implementation of the ContractNegotiationListener that signals a CountDownLatch when the
     * confirmed state has been reached.
     */
    protected class ConfirmedContractNegotiationListener implements ContractNegotiationListener {
        
        private final CountDownLatch countDownLatch;
        
        public ConfirmedContractNegotiationListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }
        
        @Override
        public void confirmed(ContractNegotiation negotiation) {
            countDownLatch.countDown();
        }
    }
    
    /**
     * Implementation of the ContractNegotiationListener that signals a CountDownLatch when the
     * declined state has been reached.
     */
    protected class DeclinedContractNegotiationListener implements ContractNegotiationListener {
        
        private final CountDownLatch countDownLatch;
        
        public DeclinedContractNegotiationListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }
        
        @Override
        public void declined(ContractNegotiation negotiation) {
            countDownLatch.countDown();
        }
    }

    /**
     * Implementation of the RemoteMessageDispatcherRegistry for the provider that delegates
     * the requests to the consumer negotiation manager directly.
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
            NegotiationResult result;
            if (message instanceof ContractOfferRequest) {
                var request = (ContractOfferRequest) message;
                result = consumerManager.offerReceived(token, request.getCorrelationId(), request.getContractOffer(), "hash");
            } else if (message instanceof ContractAgreementRequest) {
                var request = (ContractAgreementRequest) message;
                result = consumerManager.confirmed(token, request.getCorrelationId(), request.getContractAgreement(), "hash");
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
     * Implementation of the RemoteMessageDispatcherRegistry for the consumer that delegates
     * the requests to the provider negotiation manager directly.
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
            NegotiationResult result;
            if (message instanceof ContractOfferRequest) {
                var request = (ContractOfferRequest) message;
                consumerNegotiationId = request.getCorrelationId();
                result = providerManager.offerReceived(token, request.getCorrelationId(), request.getContractOffer(), "hash");
                if (NegotiationResult.Status.FATAL_ERROR.equals(result.getFailure().getStatus())) {
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

    /**
     * Monitor implementation that prints to the console.
     */
    protected class FakeConsoleMonitor implements Monitor {
        @Override
        public void debug(String message, Throwable... errors) {
            System.out.println("\u001B[34mDEBUG\u001B[0m - " + message);
            if (errors != null && errors.length > 0) {
                for (Throwable error : errors) {
                    error.printStackTrace();
                }
            }
        }

        @Override
        public void info(String message, Throwable... errors) {
            System.out.println("\u001B[32mINFO\u001B[0m - " + message);
            if (errors != null && errors.length > 0) {
                for (Throwable error : errors) {
                    error.printStackTrace();
                }
            }
        }

        @Override
        public void warning(String message, Throwable... errors) {
            System.out.println("\u001B[33mWARNING\u001B[0m - " + message);
            if (errors != null && errors.length > 0) {
                for (Throwable error : errors) {
                    error.printStackTrace();
                }
            }
        }

        @Override
        public void severe(String message, Throwable... errors) {
            System.out.println("\u001B[31mSEVERE\u001B[0m - " + message);
            if (errors != null && errors.length > 0) {
                for (Throwable error : errors) {
                    error.printStackTrace();
                }
            }
        }
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
                        .id(UUID.randomUUID().toString())
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
                        .id(UUID.randomUUID().toString())
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
                        .id(UUID.randomUUID().toString())
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
