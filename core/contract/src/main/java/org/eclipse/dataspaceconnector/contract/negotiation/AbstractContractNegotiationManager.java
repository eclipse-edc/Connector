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

package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.common.statemachine.retry.SendRetryManager;
import org.eclipse.dataspaceconnector.spi.command.CommandProcessor;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;

import java.time.Clock;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.String.format;

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
    protected int batchSize = 5;
    protected WaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    protected PolicyDefinitionStore policyStore;
    protected SendRetryManager<StatefulEntity> sendRetryManager;

    /**
     * Gives the name of the manager
     *
     * @return "Provider" for provider, "Consumer" for consumer
     */
    protected abstract String getName();

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

        public Builder<T> sendRetryManager(SendRetryManager<StatefulEntity> sendRetryManager) {
            manager.sendRetryManager = sendRetryManager;
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
            Objects.requireNonNull(manager.sendRetryManager, "sendRetryManager");
            manager.commandProcessor = new CommandProcessor<>(manager.commandQueue, manager.commandRunner, manager.monitor);

            return manager;
        }
    }

    protected void update(ContractNegotiation negotiation, Consumer<ContractNegotiationListener> observe) {
        observable.invokeForEach(observe);
        negotiationStore.save(negotiation);
    }

    protected void breakLease(ContractNegotiation negotiation) {
        negotiationStore.save(negotiation);
    }

    protected class AsyncSendResultHandler {
        private final String negotiationId;
        private final String operationDescription;
        private Consumer<ContractNegotiation> onSuccessHandler = n -> {};
        private Consumer<ContractNegotiation> onFailureHandler = n -> {};

        public AsyncSendResultHandler(String negotiationId, String operationDescription) {
            this.negotiationId = negotiationId;
            this.operationDescription = operationDescription;
        }

        public AsyncSendResultHandler onSuccess(Consumer<ContractNegotiation> onSuccessHandler) {
            this.onSuccessHandler = onSuccessHandler;
            return this;
        }

        public AsyncSendResultHandler onFailure(Consumer<ContractNegotiation> onFailureHandler) {
            this.onFailureHandler = onFailureHandler;
            return this;
        }

        public BiConsumer<Object, Throwable> build() {
            return (response, throwable) -> {
                var negotiation = negotiationStore.find(negotiationId);
                if (negotiation == null) {
                    monitor.severe(format("[%s] ContractNegotiation %s not found.", getName(), negotiationId));
                    return;
                }

                if (throwable == null) {
                    onSuccessHandler.accept(negotiation);
                    monitor.debug(format("[%s] ContractNegotiation %s is now in state %s.", getName(),
                            negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                } else if (sendRetryManager.retriesExhausted(negotiation)) {
                    negotiation.transitionError("Retry limited exceeded: " + throwable.getMessage());
                    update(negotiation, l -> l.preError(negotiation));
                    monitor.warning(format("[%s] attempt #%d failed to %s. Retry limit exceeded, ContractNegotiation %s moves to ERROR state",
                            getName(), negotiation.getStateCount(), operationDescription, negotiation.getId()), throwable);
                } else {
                    onFailureHandler.accept(negotiation);
                    monitor.warning(format("[%s] attempt #%d failed to %s. ContractNegotiation %s will stay in %s state",
                            getName(), negotiation.getStateCount(), operationDescription, negotiation.getId(),
                            ContractNegotiationStates.from(negotiation.getState())), throwable);
                }
            };
        }
    }

}
