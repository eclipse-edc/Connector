/*
 *  Copyright (c) 2021 - 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TerminateNegotiationCommandHandlerTest {

    private final ContractNegotiationListener listener = mock();
    private final ContractNegotiationObservable observable = new ContractNegotiationObservableImpl();
    private final TerminateNegotiationCommandHandler commandHandler = new TerminateNegotiationCommandHandler(mock(), observable);

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void getType_returnType() {
        assertThat(commandHandler.getType()).isEqualTo(TerminateNegotiationCommand.class);
    }

    @Test
    void handle_shouldTerminate() {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("test")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("test-protocol")
                .pending(true)
                .build();

        var command = new TerminateNegotiationCommand("test", "reason");

        var result = commandHandler.modify(negotiation, command);

        assertThat(result).isTrue();
        assertThat(negotiation.getState()).isEqualTo(TERMINATING.code());
        assertThat(negotiation.getErrorDetail()).isEqualTo("reason");
        assertThat(negotiation.isPending()).isFalse();
    }

    @Test
    void postActions_shouldNotifyTerminating() {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("test")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("test-protocol")
                .build();

        commandHandler.postActions(negotiation, new TerminateNegotiationCommand("test", "reason"));

        verify(listener).terminating(negotiation);
    }

}
