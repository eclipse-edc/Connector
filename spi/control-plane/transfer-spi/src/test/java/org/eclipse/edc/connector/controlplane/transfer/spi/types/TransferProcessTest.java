/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;


class TransferProcessTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new ObjectMapper();

        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        var writer = new StringWriter();
        mapper.writeValue(writer, process);

        var deserialized = mapper.readValue(writer.toString(), TransferProcess.class);

        assertEquals(process, deserialized);
    }

    @Test
    void verifyCopy() {
        var process = TransferProcess.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .type(TransferProcess.Type.PROVIDER)
                .createdAt(3)
                .updatedAt(1234)
                .state(TransferProcessStates.COMPLETED.code())
                .contentDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .stateCount(1)
                .stateTimestamp(1)
                .privateProperties(Map.of("k", "v"))
                .dataPlaneId("dataPlaneId")
                .transferType("transferType")
                .build();

        var copy = process.copy();

        assertEquals(process.getState(), copy.getState());
        assertEquals(process.getType(), copy.getType());
        assertEquals(process.getCreatedAt(), copy.getCreatedAt());
        assertEquals(process.getStateCount(), copy.getStateCount());
        assertEquals(process.getStateTimestamp(), copy.getStateTimestamp());
        assertEquals(process.getPrivateProperties(), copy.getPrivateProperties());
        assertEquals(process.getDataPlaneId(), copy.getDataPlaneId());
        assertEquals(process.getTransferType(), copy.getTransferType());
        assertNotNull(process.getContentDataAddress());

        assertThat(process).usingRecursiveComparison().isEqualTo(copy);
    }

    @Test
    void verifyConsumerTransitions() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(CONSUMER).build();

        process.transitionRequesting();
        process.transitionRequested();

        assertThrows(IllegalStateException.class, process::transitionStarting, "STARTING is not a valid state for consumer");
        process.transitionStarted();

        process.transitionSuspending("suspension");
        process.transitionSuspended();

        process.transitionStarted();

        process.transitionCompleting();
        process.transitionCompleted();
        assertThat(process.getErrorDetail()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, mode = INCLUDE, names = {"STARTING", "SUSPENDED"})
    void shouldNotSetDataPlaneIdOnStart_whenTransferIsConsumer(TransferProcessStates fromState) {
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).type(CONSUMER)
                .state(fromState.code())
                .build();

        process.transitionStarted();

        assertThat(process.stateAsString()).isEqualTo(STARTED.name());
        assertThat(process.getDataPlaneId()).isNull();
    }

    @Test
    void verifyProviderTransitions() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(TransferProcess.Type.PROVIDER).build();

        assertThrows(IllegalStateException.class, process::transitionRequesting, "REQUESTING is not a valid state for provider");
        assertThrows(IllegalStateException.class, process::transitionRequested, "REQUESTED is not a valid state for provider");

        process.transitionStarting();
        process.transitionStarted();

        process.transitionCompleting();
        process.transitionCompleted();
    }

    @ParameterizedTest
    @EnumSource(
            value = TransferProcessStates.class,
            mode = EXCLUDE,
            names = {"COMPLETED", "TERMINATED", "DEPROVISIONING", "DEPROVISIONING_REQUESTED", "DEPROVISIONED", "RESUMED"}
    )
    void verifyTerminating_validStates(TransferProcessStates state) {
        var transferProcess = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(state.code())
                .build();

        transferProcess.transitionTerminating("a reason");

        assertThat(transferProcess.getState()).isEqualTo(TERMINATING.code());
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, mode = INCLUDE, names = {"COMPLETED", "TERMINATED"})
    void verifyTerminating_invalidStates(TransferProcessStates state) {
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(state.code())
                .build();

        assertThatThrownBy(() -> process.transitionTerminating("a reason")).isInstanceOf(IllegalStateException.class);
    }

}
