/*
 *  Copyright (c) 2021-2022 Fraunhofer Institute for Software and Systems Engineering
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
package org.eclipse.dataspaceconnector.contract.negotiation.command;

import org.eclipse.dataspaceconnector.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.command.commands.SingleContractNegotiationCommand;
import org.eclipse.dataspaceconnector.contract.negotiation.command.handlers.SingleContractNegotiationCommandHandler;
import org.eclipse.dataspaceconnector.spi.command.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractNegotiationCommandQueueIntegrationTest {

    private ContractNegotiationStore store;
    private ContractValidationService validationService;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private CommandQueue<ContractNegotiationCommand> commandQueue;
    private CommandRunner<ContractNegotiationCommand> commandRunner;
    private ContractNegotiationObservable observable;
    private Monitor monitor;

    private CountDownLatch countDownLatch;
    private String errorDetail;

    private String negotiationId;
    private ContractNegotiation negotiation;
    private TestCommand command;

    @BeforeEach
    void setUp() {
        // Create mocks
        monitor = mock(Monitor.class);
        store = mock(ContractNegotiationStore.class);
        validationService = mock(ContractValidationService.class);
        dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        observable = mock(ContractNegotiationObservable.class);

        var commandHandlerRegistry = mock(CommandHandlerRegistry.class);

        // Set error detail that will be set on the negotiation by the command handler
        errorDetail = "Updated by command handler";

        // Create & register CommandHandler
        countDownLatch = new CountDownLatch(1);
        TestCommandHandler handler = new TestCommandHandler(store, countDownLatch, errorDetail);

        when(commandHandlerRegistry.get(TestCommand.class)).thenReturn(handler);

        // Create CommandRunner
        commandRunner = new CommandRunner<>(commandHandlerRegistry, monitor);

        // Create CommandQueue
        commandQueue = new BoundedCommandQueue<>(1);

        // Create ContractNegotiation and TestCommand
        negotiationId = "test";
        negotiation = getNegotiation(negotiationId);
        command = new TestCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(negotiation);
    }

    @Test
    void submitTestCommand_providerManager() throws Exception {
        // Create and start the negotiation manager
        var negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .build();
        negotiationManager.start(store);

        // Enqueue command
        negotiationManager.enqueueCommand(command);

        // Wait for CommandHandler to modify negotiation with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
        assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

        // Stop the negotiation manager
        negotiationManager.stop();
    }

    @Test
    void submitTestCommand_consumerManager() throws Exception {
        when(store.find(negotiationId)).thenReturn(negotiation);

        // Create and start the negotiation manager
        var negotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .build();
        negotiationManager.start(store);

        // Enqueue command
        negotiationManager.enqueueCommand(command);

        // Wait for CommandHandler to modify negotiation with time out at 15 seconds
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
        assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

        // Stop the negotiation manager
        negotiationManager.stop();
    }

    private ContractNegotiation getNegotiation(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .protocol("test-protocol")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .state(0)
                .build();
    }

    /**
     * Example Command implementation for this test.
     */
    private static class TestCommand extends SingleContractNegotiationCommand {
        public TestCommand(String negotiationId) {
            super(negotiationId);
        }
    }

    /**
     * Handler for the {@link TestCommand}. Will transition the specific {@link ContractNegotiation}
     * to the error state and set a custom error detail.
     */
    private static class TestCommandHandler extends SingleContractNegotiationCommandHandler<TestCommand> {

        private CountDownLatch countDownLatch;
        private String errorDetail;

        public TestCommandHandler(ContractNegotiationStore store, CountDownLatch countDownLatch, String errorDetail) {
            super(store);
            this.countDownLatch = countDownLatch;
            this.errorDetail = errorDetail;
        }

        @Override
        protected boolean modify(ContractNegotiation negotiation) {
            negotiation.transitionError(errorDetail);
            store.save(negotiation);
            countDownLatch.countDown();
            return true;
        }

        @Override
        public Class<TestCommand> getType() {
            return TestCommand.class;
        }
    }

}
