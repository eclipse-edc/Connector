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
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestToken;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProvisionResponsesHandlerTest {

    private final TransferProcessListener listener = mock();
    private final TransferProcessObservableImpl observable = new TransferProcessObservableImpl();
    private final Vault vault = mock();
    private final ProvisionResponsesHandler handler = new ProvisionResponsesHandler(observable, mock(), vault, mock());

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void shouldNotHandle_whenTransferProcessHasBeenAlreadyProvisioned() {
        var entity = createTransferProcessBuilder(REQUESTING)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .resource(provisionedDataDestinationResource())
                .build();

        var result = handler.handle(entity, List.of(StatusResult.success(provisionResponse)));

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(REQUESTING.code());
        verifyNoInteractions(vault, listener);
    }

    @Test
    void shouldTransitionToProvisioned_whenResourceIsDataDestination() {
        var entity = createTransferProcessBuilder(PROVISIONING)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .resource(provisionedDataDestinationResource())
                .build();

        var result = handler.handle(entity, List.of(StatusResult.success(provisionResponse)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONED.code());
        verifyNoInteractions(vault);

        handler.postActions(entity);

        verify(listener).provisioned(entity);
    }

    @Test
    void shouldTransitionToProvisioned_whenResourceIsContentAddress() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();

        var entity = createTransferProcessBuilder(PROVISIONING)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .resource(createTestProvisionedContentResource(resourceDefinition.getId()))
                .secretToken(new TestToken())
                .build();
        when(vault.storeSecret(anyString(), any(), any())).thenReturn(Result.success());

        var result = handler.handle(entity, List.of(StatusResult.success(provisionResponse)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONED.code());
        verify(vault).storeSecret(anyString(), any(), any());

        handler.postActions(entity);

        verify(listener).provisioned(entity);
    }

    @Test
    void shouldTransitionToProvisioningRequest_whenResponseIsInProcess() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var entity = createTransferProcessBuilder(PROVISIONING)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .inProcess(true)
                .build();

        var result = handler.handle(entity, List.of(StatusResult.success(provisionResponse)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONING_REQUESTED.code());
        verifyNoInteractions(vault);

        handler.postActions(entity);

        verify(listener).provisioningRequested(entity);
    }

    @Test
    void shouldTransitionToTerminating_whenProviderAndResponseIsFatalError() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var entity = createTransferProcessBuilder(PROVISIONING)
                .type(PROVIDER)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var result = handler.handle(entity, List.of(StatusResult.failure(FATAL_ERROR)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(TERMINATING.code());
        verifyNoInteractions(vault);

        handler.postActions(entity);

        verifyNoInteractions(listener);
    }

    @Test
    void shouldTransitionToTerminated_whenConsumerAndResponseIsFatalError() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var entity = createTransferProcessBuilder(PROVISIONING)
                .type(CONSUMER)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var result = handler.handle(entity, List.of(StatusResult.failure(FATAL_ERROR)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(TERMINATED.code());
        verifyNoInteractions(vault);

        handler.postActions(entity);

        verify(listener).terminated(entity);
    }

    @Test
    void shouldNotChangeStatus_whenErrorRetry() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var entity = createTransferProcessBuilder(PROVISIONING)
                .type(CONSUMER)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var result = handler.handle(entity, List.of(StatusResult.failure(ERROR_RETRY)));

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONING.code());
        verifyNoInteractions(vault);

        handler.postActions(entity);

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
                .counterPartyAddress("http://an/address")
                .correlationId(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("type")
                        .build());
    }

    private ProvisionedDataDestinationResource provisionedDataDestinationResource() {
        return new TestProvisionedDataDestinationResource("test-resource", "providsioned-resource-id");
    }


    private TestProvisionedContentResource createTestProvisionedContentResource(String resourceDefinitionId) {
        return TestProvisionedContentResource.Builder.newInstance()
                .resourceName("test")
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId(resourceDefinitionId)
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .hasToken(true)
                .build();
    }

}
