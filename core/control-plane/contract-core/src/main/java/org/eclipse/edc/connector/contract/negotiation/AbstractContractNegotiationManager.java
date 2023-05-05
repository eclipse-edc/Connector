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
    protected String participantId;
    protected ContractNegotiationStore negotiationStore;
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

    protected void transitionToInitial(ContractNegotiation negotiation) {
        negotiation.transitionInitial();
        update(negotiation);
        observable.invokeForEach(l -> l.initiated(negotiation));
    }

    protected void transitionToRequesting(ContractNegotiation negotiation) {
        negotiation.transitionRequesting();
        update(negotiation);
    }

    protected void transitionToRequested(ContractNegotiation negotiation) {
        negotiation.transitionRequested();
        update(negotiation);
        observable.invokeForEach(l -> l.requested(negotiation));
    }

    protected void transitionToAccepting(ContractNegotiation negotiation) {
        negotiation.transitionAccepting();
        update(negotiation);
    }

    protected void transitionToAccepted(ContractNegotiation negotiation) {
        negotiation.transitionAccepted();
        update(negotiation);
        observable.invokeForEach(l -> l.accepted(negotiation));
    }

    protected void transitionToOffering(ContractNegotiation negotiation) {
        negotiation.transitionOffering();
        update(negotiation);
    }

    protected void transitionToOffered(ContractNegotiation negotiation) {
        negotiation.transitionOffered();
        update(negotiation);
        observable.invokeForEach(l -> l.offered(negotiation));
    }

    protected void transitionToAgreeing(ContractNegotiation negotiation) {
        negotiation.transitionAgreeing();
        update(negotiation);
    }

    protected void transitionToAgreed(ContractNegotiation negotiation, ContractAgreement agreement) {
        negotiation.setContractAgreement(agreement);
        negotiation.transitionAgreed();
        update(negotiation);
        observable.invokeForEach(l -> l.agreed(negotiation));
    }

    protected void transitionToVerifying(ContractNegotiation negotiation) {
        negotiation.transitionVerifying();
        update(negotiation);
    }

    protected void transitionToVerified(ContractNegotiation negotiation) {
        negotiation.transitionVerified();
        update(negotiation);
        observable.invokeForEach(l -> l.verified(negotiation));
    }

    protected void transitionToFinalizing(ContractNegotiation negotiation) {
        negotiation.transitionFinalizing();
        update(negotiation);
    }

    protected void transitionToFinalized(ContractNegotiation negotiation) {
        negotiation.transitionFinalized();
        update(negotiation);
        observable.invokeForEach(l -> l.finalized(negotiation));
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
                negotiation.getType(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
    }

    public static class Builder<T extends AbstractContractNegotiationManager> {

        private final T manager;

        protected Builder(T manager) {
            this.manager = manager;
            this.manager.clock = Clock.systemUTC(); // default implementation
            this.manager.telemetry = new Telemetry(); // default noop implementation
            this.manager.executorInstrumentation = ExecutorInstrumentation.noop(); // default noop implementation
        }

        public Builder<T> participantId(String id) {
            manager.participantId = id;
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
            Objects.requireNonNull(manager.participantId, "participantId");
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
