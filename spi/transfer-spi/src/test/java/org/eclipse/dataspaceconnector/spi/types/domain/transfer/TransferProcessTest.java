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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING_REQ;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.UNSAVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TransferProcessTest {

    @Test
    void should_serialize_and_deserialize_correctly() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);
        TransferProcess deserialized = mapper.readValue(writer.toString(), TransferProcess.class);

        assertEquals(process, deserialized);
    }

    @Test
    void should_be_copied() {
        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).type(PROVIDER).state(COMPLETED.code()).stateCount(1).stateTimestamp(1)
                .build();

        TransferProcess copy = process.copy();

        assertEquals(process.getState(), copy.getState());
        assertEquals(process.getType(), copy.getType());
        assertEquals(process.getStateCount(), copy.getStateCount());
        assertEquals(process.getStateTimestamp(), copy.getStateTimestamp());
        assertEquals(process, copy);
    }

    @Test
    void should_transit_to_all_the_states_for_consumer_type() {
        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).type(CONSUMER).build();

        // test illegal transition
        assertThrows(IllegalStateException.class, () -> process.transitionProvisioning(ResourceManifest.Builder.newInstance().build()));
        process.transitionInitial();

        // test illegal transition
        assertThrows(IllegalStateException.class, process::transitionProvisioned);

        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        process.transitionProvisioned();

        process.transitionRequested();

        // test illegal transition
        assertThrows(IllegalStateException.class, process::transitionEnded);

        process.transitionRequestAck();
        process.transitionInProgress();
        process.transitionCompleted();

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
        process.transitionEnded();
    }

    @Test
    void should_transit_to_all_the_states_for_provider_type() {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(PROVIDER).build();

        process.transitionInitial();

        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        process.transitionProvisioned();

        // no request or ack on provider
        process.transitionInProgress();
        process.transitionCompleted();

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
        process.transitionEnded();
    }

    @Test
    void should_rollback_state() {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        process.transitionInitial();
        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        process.rollbackState(TransferProcessStates.INITIAL);

        assertEquals(TransferProcessStates.INITIAL.code(), process.getState());
        assertEquals(1, process.getStateCount());
    }

    @Test
    void provisioning_should_complete_when_every_definition_has_its_own_resource() {
        ResourceManifest manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDefinition.Builder.newInstance().id("r1").build()))
                .build();

        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceManifest(manifest)
                .build();

        assertFalse(process.provisioningComplete());

        ProvisionedResourceSet resourceSet = ProvisionedResourceSet.Builder.newInstance().build();

        TransferProcess provisionedProcess = process.toBuilder().provisionedResourceSet(resourceSet).build();

        assertFalse(provisionedProcess.provisioningComplete());

        resourceSet.addResource(TestProvisionedResource.Builder.newInstance().id("p1").resourceDefinitionId("r1").transferProcessId("123").build());

        assertTrue(provisionedProcess.provisioningComplete());
    }

    @Test
    void should_pass_from_deprovisioning_request_to_deprovisioning() {
        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).state(DEPROVISIONING_REQ.code()).build();

        process.transitionDeprovisioning();

        assertThat(process.getState()).isEqualTo(DEPROVISIONING.code());
    }

    @Test
    void should_transition_to_error_after_state_count_threshold_is_crossed() {
        int stateCountThreshold = 3;
        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).state(INITIAL.code()).stateCountThreshold(stateCountThreshold).build();
        ResourceManifest resourceManifest = ResourceManifest.Builder.newInstance().build();
        range(0, stateCountThreshold).forEach(i -> process.transitionProvisioning(resourceManifest));

        process.transitionProvisioning(resourceManifest);

        assertThat(process.getState()).isEqualTo(TransferProcessStates.ERROR.code());
        assertThat(process.getErrorDetail()).isNotBlank();
    }

    @Test
    void should_transition_to_error_after_the_state_timeout_threshold_is_crossed() {
        ResourceManifest resourceManifest = ResourceManifest.Builder.newInstance().build();
        TransferProcess process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(PROVISIONING.code())
                .stateTimeoutThreshold(1)
                .build();

        process.transitionProvisioning(resourceManifest);

        assertThat(process.getState()).isEqualTo(TransferProcessStates.ERROR.code());
        assertThat(process.getErrorDetail()).isNotBlank();
    }
}
