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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
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

        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        process.transitionProvisioned();

        process.transitionRequesting();
        process.transitionRequested();

        assertThrows(IllegalStateException.class, process::transitionStarting, "STARTING is not a valid state for consumer");
        process.transitionStarted("dataPlaneId");
        // should not set the data plane id
        assertThat(process.getDataPlaneId()).isNull();

        process.transitionSuspending("suspension");
        process.transitionSuspended();

        process.transitionStarted("dataPlaneId");
        // should not set the data plane id
        assertThat(process.getDataPlaneId()).isNull();

        process.transitionCompleting();
        process.transitionCompleted();

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
    }

    @Test
    void verifyProviderTransitions() {
        var process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).type(TransferProcess.Type.PROVIDER).build();

        process.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        process.transitionProvisioned();

        assertThrows(IllegalStateException.class, process::transitionRequesting, "REQUESTING is not a valid state for provider");
        assertThrows(IllegalStateException.class, process::transitionRequested, "REQUESTED is not a valid state for provider");

        process.transitionStarting();
        process.transitionStarted("dataPlaneId");
        // should set the data plane id
        assertThat(process.getDataPlaneId()).isEqualTo("dataPlaneId");


        process.transitionCompleting();
        process.transitionCompleted();

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
    }

    @ParameterizedTest
    @EnumSource(
            value = TransferProcessStates.class,
            mode = EXCLUDE,
            names = { "COMPLETED", "TERMINATED", "DEPROVISIONING", "DEPROVISIONING_REQUESTED", "DEPROVISIONED", "RESUMED" }
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
    @EnumSource(value = TransferProcessStates.class, mode = INCLUDE, names = { "COMPLETED", "TERMINATED" })
    void verifyTerminating_invalidStates(TransferProcessStates state) {
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(state.code())
                .build();

        assertThatThrownBy(() -> process.transitionTerminating("a reason")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyProvisioningComplete() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDefinition.Builder.newInstance().id("r1").build()))
                .build();

        var notCompleted = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceManifest(manifest)
                .build();

        assertThat(notCompleted.provisioningComplete()).isFalse();

        var stillNotCompleted = notCompleted.toBuilder()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .build();

        assertThat(stillNotCompleted.provisioningComplete()).isFalse();

        var completed = stillNotCompleted.toBuilder()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(TestProvisionedResource.Builder.newInstance().id("p1").resourceDefinitionId("r1").transferProcessId("123").build()))
                        .build())
                .build();

        assertThat(completed.provisioningComplete()).isTrue();
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

        assertThat(process.getResourcesToProvision()).hasSize(1);
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

        assertThat(process.getResourcesToProvision()).hasSize(1);
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

        assertThat(process.getResourcesToDeprovision()).hasSize(1);
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
