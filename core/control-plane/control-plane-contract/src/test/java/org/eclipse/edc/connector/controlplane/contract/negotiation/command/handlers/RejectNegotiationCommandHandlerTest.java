/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.RejectNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class RejectNegotiationCommandHandlerTest {

    private final ContractNegotiationListener listener = mock();
    private final ContractNegotiationObservable observable = new ContractNegotiationObservableImpl();
    private final RejectNegotiationCommandHandler commandHandler = new RejectNegotiationCommandHandler(mock(), observable);

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void getType_returnType() {
        assertThat(commandHandler.getType()).isEqualTo(RejectNegotiationCommand.class);
    }

    @Test
    void modify_shouldTransition_whenProviderRequestedAndPending() {
        var negotiation = negotiation(PROVIDER, REQUESTED, true);

        var result = commandHandler.modify(negotiation, new RejectNegotiationCommand("test"));

        assertThat(result).isTrue();
        assertThat(negotiation.getState()).isEqualTo(TERMINATING.code());
        assertThat(negotiation.isPending()).isFalse();
        assertThat(negotiation.getErrorDetail()).isEqualTo(RejectNegotiationCommandHandler.REJECTION_REASON);
    }

    @Test
    void modify_shouldNotTransition_whenConsumer() {
        var negotiation = negotiation(CONSUMER, REQUESTED, true);

        var result = commandHandler.modify(negotiation, new RejectNegotiationCommand("test"));

        assertThat(result).isFalse();
        assertThat(negotiation.getState()).isEqualTo(REQUESTED.code());
        assertThat(negotiation.isPending()).isTrue();
    }

    @Test
    void modify_shouldNotTransition_whenNotPending() {
        var negotiation = negotiation(PROVIDER, REQUESTED, false);

        var result = commandHandler.modify(negotiation, new RejectNegotiationCommand("test"));

        assertThat(result).isFalse();
        assertThat(negotiation.getState()).isEqualTo(REQUESTED.code());
    }

    @ParameterizedTest
    @EnumSource(value = ContractNegotiationStates.class, mode = EXCLUDE, names = "REQUESTED")
    void modify_shouldNotTransition_whenNotRequested(ContractNegotiationStates state) {
        var negotiation = negotiation(PROVIDER, state, true);

        var result = commandHandler.modify(negotiation, new RejectNegotiationCommand("test"));

        assertThat(result).isFalse();
        assertThat(negotiation.getState()).isEqualTo(state.code());
        assertThat(negotiation.isPending()).isTrue();
    }

    @Test
    void postActions_shouldNotifyListeners() {
        var negotiation = negotiation(PROVIDER, TERMINATING, false);

        commandHandler.postActions(negotiation, new RejectNegotiationCommand("test"));

        var inOrder = inOrder(listener);
        inOrder.verify(listener).rejected(negotiation);
        inOrder.verify(listener).terminating(negotiation);
    }

    private ContractNegotiation negotiation(ContractNegotiation.Type type, ContractNegotiationStates state, boolean pending) {
        return ContractNegotiation.Builder.newInstance()
                .id("test")
                .type(type)
                .state(state.code())
                .pending(pending)
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("test-protocol")
                .build();
    }

}
