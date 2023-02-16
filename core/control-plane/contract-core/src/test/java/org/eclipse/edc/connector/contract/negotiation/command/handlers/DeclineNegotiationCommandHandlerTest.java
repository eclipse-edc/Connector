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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.command.DeclineNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeclineNegotiationCommandHandlerTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final Clock now = Clock.systemUTC();
    private final Clock future = Clock.offset(now, Duration.ofMinutes(1));
    private DeclineNegotiationCommandHandler commandHandler;

    @BeforeEach
    public void setUp() {
        commandHandler = new DeclineNegotiationCommandHandler(store);
    }

    @Test
    void handle_negotiationExists_declineNegotiation() {
        var negotiationId = "test";
        var negotiation = ContractNegotiation.Builder.newInstance().id(negotiationId).counterPartyId("counter-party").counterPartyAddress("https://counter-party").protocol("test-protocol").state(ContractNegotiationStates.REQUESTED.code()).clock(future).updatedAt(now.millis()).build();
        var originalTime = negotiation.getUpdatedAt();
        var command = new DeclineNegotiationCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(negotiation);

        var result = commandHandler.handle(command);

        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.DECLINING.code());
        assertThat(negotiation.getErrorDetail()).isNotBlank();
        assertThat(negotiation.getUpdatedAt()).isGreaterThan(originalTime);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void handle_negotiationDoesNotExist_throwEdcException() {
        var negotiationId = "test";
        var command = new DeclineNegotiationCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(null);

        assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(EdcException.class).hasMessage(format("Could not find ContractNegotiation with ID [%s]", negotiationId));
    }

    @Test
    void getType_returnType() {
        assertThat(commandHandler.getType()).isEqualTo(DeclineNegotiationCommand.class);
    }

}
