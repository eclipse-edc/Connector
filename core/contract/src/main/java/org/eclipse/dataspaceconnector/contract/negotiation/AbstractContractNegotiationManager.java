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

import org.eclipse.dataspaceconnector.spi.command.CommandProcessor;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;

import java.time.Clock;
import java.util.Objects;

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

            return manager;
        }
    }

}
