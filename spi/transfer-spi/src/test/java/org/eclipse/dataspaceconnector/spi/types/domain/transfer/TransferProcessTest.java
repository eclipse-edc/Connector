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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
                .createdTimestamp(3)
                .state(TransferProcessStates.COMPLETED.code())
                .contentDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .stateCount(1)
                .stateTimestamp(1)
                .build();

        var copy = process.copy();

        assertEquals(process.getState(), copy.getState());
        assertEquals(process.getType(), copy.getType());
        assertEquals(process.getCreatedTimestamp(), copy.getCreatedTimestamp());
        assertEquals(process.getStateCount(), copy.getStateCount());
        assertEquals(process.getStateTimestamp(), copy.getStateTimestamp());
        assertNotNull(process.getContentDataAddress());

        assertEquals(process, copy);
    }

    @Test
    void verifyConsumerTransitions() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(TransferProcess.Type.CONSUMER).build();

        // test illegal transition
        assertThrows(IllegalStateException.class, () -> process.transitionProvisioning(ResourceManifest.Builder.newInstance().build()));
        process.transitionInitial();

        // test illegal transition
        assertThrows(IllegalStateException.class, process::transitionProvisioned);

        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        process.transitionProvisioned();

        process.transitionRequesting();
        process.transitionRequested();

        // test illegal transition
        assertThrows(IllegalStateException.class, process::transitionEnded);

        process.transitionInProgress();
        process.transitionCompleted();

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
        process.transitionEnded();
    }

    @Test
    void verifyProviderTransitions() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(TransferProcess.Type.PROVIDER).build();

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
    void verifyTransitionRollback() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        process.transitionInitial();
        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        process.rollbackState(INITIAL);

        assertEquals(INITIAL.code(), process.getState());
        assertEquals(1, process.getStateCount());
    }

    @Test
    void verifyProvisioningComplete() {
        var builder = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString());

        var manifest = ResourceManifest.Builder.newInstance().build();
        manifest.addDefinition(TestResourceDefinition.Builder.newInstance().id("r1").build());

        var process = builder.resourceManifest(manifest).build();

        assertFalse(process.provisioningComplete());

        ProvisionedResourceSet resourceSet = ProvisionedResourceSet.Builder.newInstance().build();

        process = process.toBuilder().provisionedResourceSet(resourceSet).build();

        assertFalse(process.provisioningComplete());

        resourceSet.addResource(TestProvisionedResource.Builder.newInstance().id("p1").resourceDefinitionId("r1").transferProcessId("123").build());

        assertTrue(process.provisioningComplete());
    }

    @Test
    void verifyResourceToProvisionWhenEmptyResources() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        assertThat(process.getResourcesToProvision()).isEmpty();
    }

    @Test
    void verifyResourceToProvisionResultsHaveNotBeenReturned() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDefinition.Builder.newInstance().id("1").build()))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceManifest(manifest)
                .build();

        assertThat(process.getResourcesToProvision().size()).isEqualTo(1);
    }

    @Test
    void verifyResourceToProvisionResultsHaveBeenReturned() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDefinition.Builder.newInstance().id("1").build()))
                .definitions(List.of(TestResourceDefinition.Builder.newInstance().id("2").build()))
                .build();

        var provisionedResource = TestProvisionedResource.Builder.newInstance()
                .id("123")
                .transferProcessId("4")
                .resourceDefinitionId("1")
                .build();

        var provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(provisionedResource))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceManifest(manifest)
                .provisionedResourceSet(provisionedResourceSet)
                .build();

        assertThat(process.getResourcesToProvision().size()).isEqualTo(1);
    }

    @Test
    void verifyResourceToDeprovisionWhenEmptyResources() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        assertThat(process.getResourcesToDeprovision()).isEmpty();
    }

    @Test
    void verifyResourceToDeprovisionResultsHaveBeenReturned() {
        var set = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(TestProvisionedResource.Builder.newInstance().id("1").transferProcessId("2").resourceDefinitionId("3").build()))
                .resources(List.of(TestProvisionedResource.Builder.newInstance().id("4").transferProcessId("5").resourceDefinitionId("6").build()))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .provisionedResourceSet(set)
                .deprovisionedResources(List.of(DeprovisionedResource.Builder.newInstance().provisionedResourceId("1").build()))
                .build();

        assertThat(process.getResourcesToDeprovision().size()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, names = {"COMPLETED", "ENDED", "ERROR"}, mode = EnumSource.Mode.EXCLUDE)
    void verifyCancel_validStates(TransferProcessStates state) {
        var builder = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString());
        builder.state(state.code());
        var tp = builder.build();
        tp.transitionCancelled();
        assertThat(tp.getState()).isEqualTo(TransferProcessStates.CANCELLED.code());

    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, names = {"COMPLETED", "ENDED", "ERROR"}, mode = EnumSource.Mode.INCLUDE)
    void verifyCancel_invalidStates(TransferProcessStates state) {
        var builder = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString());
        builder.state(state.code());
        var tp = builder.build();
        assertThatThrownBy(tp::transitionCancelled).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should considered provisioned when there are no definitions and no provisioned resource")
    void provisionComplete_emptyManifestAndResources() {
        var emptyManifest = ResourceManifest.Builder.newInstance().definitions(emptyList()).build();
        var emptyResources = ProvisionedResourceSet.Builder.newInstance().resources(emptyList()).build();
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).resourceManifest(emptyManifest).provisionedResourceSet(emptyResources)
                .build();

        var provisioningComplete = process.provisioningComplete();

        assertThat(provisioningComplete).isTrue();
    }

    @Test
    @DisplayName("Should considered provisioned when there are no definitions and provisioned resource set is null")
    void provisionComplete_noResources() {
        var emptyManifest = ResourceManifest.Builder.newInstance().definitions(emptyList()).build();
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).resourceManifest(emptyManifest).provisionedResourceSet(null)
                .build();

        var provisioningComplete = process.provisioningComplete();

        assertThat(provisioningComplete).isTrue();
    }

    @Test
    void verifyGetProvisionedResource() {
        var provisionedResource = TestProvisionedResource.Builder.newInstance()
                .id("123")
                .transferProcessId("4")
                .resourceDefinitionId("1")
                .build();

        var provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(provisionedResource))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id("1")
                .provisionedResourceSet(provisionedResourceSet)
                .build();

        assertThat(process.getProvisionedResource("123")).isNotNull();
    }

    @Test
    void verifyDeprovisionNoResources() {
        var process = TransferProcess.Builder.newInstance().id("1").build();
        assertThat(process.deprovisionComplete()).isTrue();
    }

    @Test
    void verifyDeprovisionComplete() {
        var provisionedResource = TestProvisionedResource.Builder.newInstance()
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId("3")
                .build();

        var provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(provisionedResource))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id("1")
                .provisionedResourceSet(provisionedResourceSet)
                .build();

        process.addDeprovisionedResource(DeprovisionedResource.Builder.newInstance().provisionedResourceId("1").build());

        assertThat(process.deprovisionComplete()).isTrue();
    }


    @Test
    void verifyDeprovisionNotComplete() {
        var provisionedResource1 = TestProvisionedResource.Builder.newInstance()
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId("3")
                .build();

        var provisionedResource2 = TestProvisionedResource.Builder.newInstance()
                .id("4")
                .transferProcessId("5")
                .resourceDefinitionId("6")
                .build();

        var provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(provisionedResource1))
                .resources(List.of(provisionedResource2))
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id("1")
                .provisionedResourceSet(provisionedResourceSet)
                .build();

        process.addDeprovisionedResource(DeprovisionedResource.Builder.newInstance().provisionedResourceId("4").build());

        assertThat(process.deprovisionComplete()).isFalse();
    }


}
