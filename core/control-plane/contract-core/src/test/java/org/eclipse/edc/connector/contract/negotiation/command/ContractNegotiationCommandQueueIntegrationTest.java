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
 *
 */

package org.eclipse.edc.connector.contract.negotiation.command;

import org.eclipse.edc.connector.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.edc.connector.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.edc.connector.contract.negotiation.command.handlers.SingleContractNegotiationCommandHandler;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.command.SingleContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.command.BoundedCommandQueue;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractNegotiationCommandQueueIntegrationTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final ContractNegotiationObservable observable = mock(ContractNegotiationObservable.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final Monitor monitor = mock(Monitor.class);
    private final String errorDetail = "Updated by command handler";
    private CommandQueue<ContractNegotiationCommand> commandQueue;
    private CommandRunner<ContractNegotiationCommand> commandRunner;
    private String negotiationId;
    private ContractNegotiation negotiation;
    private TestCommand command;
    private SendRetryManager sendRetryManager = mock(SendRetryManager.class);

    @BeforeEach
    void setUp() {
        var commandHandlerRegistry = mock(CommandHandlerRegistry.class);

        TestCommandHandler handler = new TestCommandHandler(store, errorDetail);

        when(commandHandlerRegistry.get(TestCommand.class)).thenReturn(handler);

        commandRunner = new CommandRunner<>(commandHandlerRegistry, monitor);

        commandQueue = new BoundedCommandQueue<>(1);

        negotiationId = "test";
        negotiation = getNegotiation(negotiationId);
        command = new TestCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(negotiation);
    }

    @Test
    void submitTestCommand_providerManager() {
        var negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();

        negotiationManager.start();

        negotiationManager.enqueueCommand(command);


        await().untilAsserted(() -> {
            assertThat(negotiation.getState()).isEqualTo(TERMINATING.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);
        });

        // Stop the negotiation manager
        negotiationManager.stop();
    }

    @Test
    void submitTestCommand_consumerManager() {
        when(store.find(negotiationId)).thenReturn(negotiation);

        // Create and start the negotiation manager
        var negotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();
        negotiationManager.start();

        negotiationManager.enqueueCommand(command);

        await().untilAsserted(() -> {
            assertThat(negotiation.getState()).isEqualTo(TERMINATING.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);
        });

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
        TestCommand(String negotiationId) {
            super(negotiationId);
        }
    }

    /**
     * Handler for the {@link TestCommand}. Will transition the specific {@link ContractNegotiation} to the terminating state
     * and set a custom error detail.
     */
    private static class TestCommandHandler extends SingleContractNegotiationCommandHandler<TestCommand> {

        private final String errorDetail;

        TestCommandHandler(ContractNegotiationStore store, String errorDetail) {
            super(store);
            this.errorDetail = errorDetail;
        }

        @Override
        public Class<TestCommand> getType() {
            return TestCommand.class;
        }

        @Override
        protected boolean modify(ContractNegotiation negotiation) {
            negotiation.transitionTerminating(errorDetail);
            store.save(negotiation);
            return true;
        }
    }

}
