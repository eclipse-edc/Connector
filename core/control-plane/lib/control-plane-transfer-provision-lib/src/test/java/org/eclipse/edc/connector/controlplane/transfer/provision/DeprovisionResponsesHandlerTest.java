/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.provision;

import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TokenTestProvisionResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.ParticipantVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeprovisionResponsesHandlerTest {

    private final ParticipantVault vault = mock();
    private final TransferProcessObservableImpl observable = new TransferProcessObservableImpl();
    private final DeprovisionResponsesHandler handler = new DeprovisionResponsesHandler(observable, mock(), vault);
    private final TransferProcessListener listener = mock();

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void shouldTransitionToDeprovisioned() {
        var provisionedResourceId = "provisioned-resource-id";
        var manifest = ResourceManifest.Builder.newInstance()
                               .definitions(List.of(new TestResourceDefinition()))
                               .build();
        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                                  .resources(List.of(new TokenTestProvisionResource("test-resource", provisionedResourceId)))
                                  .build();
        var process = createTransferProcessBuilder(DEPROVISIONING)
                              .resourceManifest(manifest)
                              .provisionedResourceSet(resourceSet)
                              .build();
        var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                                            .provisionedResourceId(provisionedResourceId)
                                            .build();
        when(vault.deleteSecret(any(), any())).thenReturn(Result.success());

        var result = handler.handle(process, List.of(StatusResult.success(deprovisionedResource)));

        assertThat(result).isTrue();
        assertThat(process.getState()).isEqualTo(DEPROVISIONED.code());
        verify(vault).deleteSecret(any(), eq("test-resource"));

        handler.postActions(process);

        verify(listener).deprovisioned(process);
    }

    @Test
    void shouldTransitionToDeprovisionRequestedOnResponseStarted() {
        var provisionedResourceId = "provisioned-resource-id";
        var manifest = ResourceManifest.Builder.newInstance()
                               .definitions(List.of(new TestResourceDefinition()))
                               .build();
        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                                  .resources(List.of(new TokenTestProvisionResource("test-resource", provisionedResourceId)))
                                  .build();
        var process = createTransferProcessBuilder(DEPROVISIONING)
                              .resourceManifest(manifest)
                              .provisionedResourceSet(resourceSet)
                              .build();
        var deprovisionedResponse = DeprovisionedResource.Builder.newInstance()
                                            .provisionedResourceId("any")
                                            .inProcess(true)
                                            .build();

        var result = handler.handle(process, List.of(StatusResult.success(deprovisionedResponse)));

        assertThat(result).isTrue();
        assertThat(process.getState()).isEqualTo(DEPROVISIONING_REQUESTED.code());
        verifyNoInteractions(vault);

        handler.postActions(process);

        verify(listener).deprovisioningRequested(process);
    }

    @Test
    void shouldTransitionToDeprovisionedWithErrorOnFatalDeprovisionError() {
        var provisionedResourceId = "provisioned-resource-id";
        var manifest = ResourceManifest.Builder.newInstance()
                               .definitions(List.of(new TestResourceDefinition()))
                               .build();
        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                                  .resources(List.of(new TokenTestProvisionResource("test-resource", provisionedResourceId)))
                                  .build();
        var process = createTransferProcessBuilder(DEPROVISIONING)
                              .resourceManifest(manifest)
                              .provisionedResourceSet(resourceSet)
                              .build();

        var result = handler.handle(process, List.of(StatusResult.failure(FATAL_ERROR)));

        assertThat(result).isTrue();
        assertThat(process.getState()).isEqualTo(DEPROVISIONED.code());
        assertThat(process.getErrorDetail()).isNotBlank();
        verifyNoInteractions(vault);

        handler.postActions(process);

        verify(listener).deprovisioned(process);
    }

    @Test
    void shouldNotChangeStateOnErrorRetry() {
        var provisionedResourceId = "provisioned-resource-id";
        var manifest = ResourceManifest.Builder.newInstance()
                               .definitions(List.of(new TestResourceDefinition()))
                               .build();
        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                                  .resources(List.of(new TokenTestProvisionResource("test-resource", provisionedResourceId)))
                                  .build();
        var process = createTransferProcessBuilder(DEPROVISIONING)
                              .resourceManifest(manifest)
                              .provisionedResourceSet(resourceSet)
                              .build();

        var result = handler.handle(process, List.of(StatusResult.failure(ERROR_RETRY)));

        assertThat(result).isTrue();
        assertThat(process.getState()).isEqualTo(DEPROVISIONING.code());
        verifyNoInteractions(vault);

        handler.postActions(process);

        verifyNoInteractions(listener);
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState) {
        var processId = UUID.randomUUID().toString();

        return TransferProcess.Builder.newInstance()
                       .participantContextId("test-participant-id")
                       .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                       .type(CONSUMER)
                       .id("test-process-" + processId)
                       .state(inState.code())
                       .protocol("protocol")
                       .counterPartyAddress("http://an/address");
    }

}
