/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.negotiation;

import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.command.CommandProcessor;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Objects;

import static org.eclipse.edc.connector.contract.ContractCoreExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.contract.ContractCoreExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.contract.ContractCoreExtension.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.connector.contract.ContractCoreExtension.DEFAULT_SEND_RETRY_LIMIT;

public abstract class AbstractContractNegotiationManager {

    protected ContractNegotiationStore negotiationStore;
    protected ContractValidationService validationService;
    protected RemoteMessageDispatcherRegistry dispatcherRegistry;
    protected ContractNegotiationObservable observable;
    protected CommandQueue<ContractNegotiationCommand> commandQueue;
    protected CommandRunner<ContractNegotiationCommand> commandRunner;
    protected CommandProcessor<ContractNegotiationCommand> commandProcessor;
    protected Monitor monitor;
    protected Clock clock;
    protected Telemetry telemetry;
    protected ExecutorInstrumentation executorInstrumentation;
    protected int batchSize = DEFAULT_BATCH_SIZE;
    protected WaitStrategy waitStrategy = () -> DEFAULT_ITERATION_WAIT;
    protected PolicyDefinitionStore policyStore;
    protected EntityRetryProcessFactory entityRetryProcessFactory;
    protected EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();

    /**
     * Gives the type of the manager
     *
     * @return "Provider" for provider, "Consumer" for consumer
     */
    protected abstract String getType();

    protected void transitionToInitial(ContractNegotiation negotiation) {
        negotiation.transitionInitial();
        update(negotiation);
        observable.invokeForEach(l -> l.initiated(negotiation));
    }

    protected void transitionToConsumerRequesting(ContractNegotiation negotiation) {
        negotiation.transitionConsumerRequesting();
        update(negotiation);
    }

    protected void transitionToConsumerRequested(ContractNegotiation negotiation) {
        negotiation.transitionConsumerRequested();
        update(negotiation);
        observable.invokeForEach(l -> l.consumerRequested(negotiation));
    }

    protected void transitionToConsumerAgreeing(ContractNegotiation negotiation) {
        negotiation.transitionConsumerAgreeing();
        update(negotiation);
    }

    protected void transitionToConsumerAgreed(ContractNegotiation negotiation) {
        negotiation.transitionConsumerAgreed();
        update(negotiation);
        observable.invokeForEach(l -> l.consumerAgreed(negotiation));
    }

    protected void transitionToProviderOffering(ContractNegotiation negotiation) {
        negotiation.transitionProviderOffering();
        update(negotiation);
    }

    protected void transitionToProviderOffered(ContractNegotiation negotiation) {
        negotiation.transitionProviderOffered();
        update(negotiation);
        observable.invokeForEach(l -> l.providerOffered(negotiation));
    }

    protected void transitionToProviderAgreeing(ContractNegotiation negotiation) {
        negotiation.transitionProviderAgreeing();
        update(negotiation);
    }

    protected void transitionToProviderAgreed(ContractNegotiation negotiation, ContractAgreement agreement) {
        negotiation.setContractAgreement(agreement);
        negotiation.transitionProviderAgreed();
        update(negotiation);
        observable.invokeForEach(l -> l.providerAgreed(negotiation));
    }

    protected void transitionToConsumerVerifying(ContractNegotiation negotiation) {
        negotiation.transitionConsumerVerifying();
        update(negotiation);
    }

    protected void transitionToConsumerVerified(ContractNegotiation negotiation) {
        negotiation.transitionConsumerVerified();
        update(negotiation);
        observable.invokeForEach(l -> l.consumerVerified(negotiation));
    }

    protected void transitionToProviderFinalizing(ContractNegotiation negotiation) {
        negotiation.transitionProviderFinalizing();
        update(negotiation);
    }

    protected void transitionToProviderFinalized(ContractNegotiation negotiation) {
        negotiation.transitionProviderFinalized();
        update(negotiation);
        observable.invokeForEach(l -> l.providerFinalized(negotiation));
    }

    protected void transitionToTerminating(ContractNegotiation negotiation, String message) {
        negotiation.transitionTerminating(message);
        update(negotiation);
    }

    protected void transitionToTerminating(ContractNegotiation negotiation) {
        negotiation.transitionTerminating();
        update(negotiation);
    }

    protected void transitionToTerminated(ContractNegotiation negotiation, String message) {
        negotiation.setErrorDetail(message);
        transitionToTerminated(negotiation);
    }

    protected void transitionToTerminated(ContractNegotiation negotiation) {
        negotiation.transitionTerminated();
        update(negotiation);
        observable.invokeForEach(l -> l.terminated(negotiation));
    }

    private void update(ContractNegotiation negotiation) {
        negotiationStore.save(negotiation);
        monitor.debug(String.format("[%s] ContractNegotiation %s is now in state %s.",
                getType(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
    }

    public static class Builder<T extends AbstractContractNegotiationManager> {

        private final T manager;

        protected Builder(T manager) {
            this.manager = manager;
            this.manager.clock = Clock.systemUTC(); // default implementation
            this.manager.telemetry = new Telemetry(); // default noop implementation
            this.manager.executorInstrumentation = ExecutorInstrumentation.noop(); // default noop implementation
        }

        public Builder<T> validationService(ContractValidationService validationService) {
            manager.validationService = validationService;
            return this;
        }

        public Builder<T> monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder<T> batchSize(int batchSize) {
            manager.batchSize = batchSize;
            return this;
        }

        public Builder<T> waitStrategy(WaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<T> dispatcherRegistry(RemoteMessageDispatcherRegistry dispatcherRegistry) {
            manager.dispatcherRegistry = dispatcherRegistry;
            return this;
        }

        public Builder<T> commandQueue(CommandQueue<ContractNegotiationCommand> commandQueue) {
            manager.commandQueue = commandQueue;
            return this;
        }

        public Builder<T> commandRunner(CommandRunner<ContractNegotiationCommand> commandRunner) {
            manager.commandRunner = commandRunner;
            return this;
        }

        public Builder<T> clock(Clock clock) {
            manager.clock = clock;
            return this;
        }

        public Builder<T> telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
            return this;
        }

        public Builder<T> executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder<T> observable(ContractNegotiationObservable observable) {
            manager.observable = observable;
            return this;
        }

        public Builder<T> store(ContractNegotiationStore store) {
            manager.negotiationStore = store;
            return this;
        }

        public Builder<T> policyStore(PolicyDefinitionStore policyStore) {
            manager.policyStore = policyStore;
            return this;
        }

        public Builder<T> entityRetryProcessConfiguration(EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
            manager.entityRetryProcessConfiguration = entityRetryProcessConfiguration;
            return this;
        }

        public T build() {
            Objects.requireNonNull(manager.validationService, "contractValidationService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.commandQueue, "commandQueue");
            Objects.requireNonNull(manager.commandRunner, "commandRunner");
            Objects.requireNonNull(manager.observable, "observable");
            Objects.requireNonNull(manager.clock, "clock");
            Objects.requireNonNull(manager.telemetry, "telemetry");
            Objects.requireNonNull(manager.executorInstrumentation, "executorInstrumentation");
            Objects.requireNonNull(manager.negotiationStore, "store");
            Objects.requireNonNull(manager.policyStore, "policyStore");

            manager.commandProcessor = new CommandProcessor<>(manager.commandQueue, manager.commandRunner, manager.monitor);
            manager.entityRetryProcessFactory = new EntityRetryProcessFactory(manager.monitor, manager.clock, manager.entityRetryProcessConfiguration);

            return manager;
        }
    }

    protected void breakLease(ContractNegotiation negotiation) {
        negotiationStore.save(negotiation);
    }


    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(DEFAULT_SEND_RETRY_LIMIT, () -> new ExponentialWaitStrategy(DEFAULT_SEND_RETRY_BASE_DELAY));

    }
}
