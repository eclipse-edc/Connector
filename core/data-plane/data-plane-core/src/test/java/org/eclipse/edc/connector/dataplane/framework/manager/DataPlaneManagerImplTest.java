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

package org.eclipse.edc.connector.dataplane.framework.manager;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceStates;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.spi.DataFlow.TERMINATION_REASON;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISION_FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISION_REQUESTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.NOTIFIED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISION_NOTIFYING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISION_REQUESTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.SUSPENDED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.matchers.ArrayContainsMatcher.arrayContains;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.response.StatusResult.failure;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class DataPlaneManagerImplTest {

    private static final int RETRY_LIMIT = 1;
    private static final int RETRY_EXHAUSTED = RETRY_LIMIT + 1;

    private final TransferService transferService = mock();
    private final TransferProcessApiClient transferProcessApiClient = mock();
    private final DataPlaneStore store = mock();
    private final TransferServiceRegistry registry = mock();
    private final EndpointDataReferenceServiceRegistry endpointDataReferenceServiceRegistry = mock();
    private final ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager = mock();
    private final ProvisionerManager provisionerManager = mock();
    private final String runtimeId = UUID.randomUUID().toString();
    private DataPlaneManager manager;

    @BeforeEach
    public void setUp() {
        when(registry.resolveTransferService(any())).thenReturn(transferService);
        manager = DataPlaneManagerImpl.Builder.newInstance()
                .executorInstrumentation(ExecutorInstrumentation.noop())
                .transferServiceRegistry(registry)
                .store(store)
                .transferProcessClient(transferProcessApiClient)
                .endpointDataReferenceServiceRegistry(endpointDataReferenceServiceRegistry)
                .resourceDefinitionGeneratorManager(resourceDefinitionGeneratorManager)
                .provisionerManager(provisionerManager)
                .monitor(mock())
                .runtimeId(runtimeId)
                .waitStrategy(() -> 10000L)
                .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L)))
                .build();
    }

    @Nested
    class Validate {

        @Test
        void shouldNotValidatePullTransfers() {
            var request = dataFlowStartMessageBuilder().flowType(PULL).build();

            var result = manager.validate(request);

            assertThat(result).isSucceeded();
            verifyNoInteractions(registry, resourceDefinitionGeneratorManager);
        }

        @Test
        void shouldValidatePushAgainstProvisionTypes_whenNoTransferServiceAvailable() {
            var source = DataAddress.Builder.newInstance().type("ProvisionType").build();
            var request = dataFlowStartMessageBuilder().flowType(PUSH).sourceDataAddress(source).build();
            when(registry.resolveTransferService(any())).thenReturn(null);
            when(resourceDefinitionGeneratorManager.sourceTypes()).thenReturn(Set.of("ProvisionType"));

            var result = manager.validate(request);

            assertThat(result).isSucceeded();
            verify(resourceDefinitionGeneratorManager).sourceTypes();
        }

        @Test
        void shouldFailWhenValidatePushAgainstProvisionTypes_whenTypeNotSupported() {
            var source = DataAddress.Builder.newInstance().type("UnknownProvisionType").build();
            var request = dataFlowStartMessageBuilder().flowType(PUSH).sourceDataAddress(source).build();
            when(registry.resolveTransferService(any())).thenReturn(null);
            when(resourceDefinitionGeneratorManager.sourceTypes()).thenReturn(Set.of("ProvisionType"));

            var result = manager.validate(request);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class StartNewFlow {

        @BeforeEach
        void setUp() {
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.notFound("flow does not exist"));
        }

        @Test
        void shouldInitiatePushDataFlow() {
            var request = dataFlowStartMessageBuilder().flowType(PUSH).build();

            manager.start(request);

            var captor = ArgumentCaptor.forClass(DataFlow.class);
            verify(store).save(captor.capture());
            var dataFlow = captor.getValue();
            assertThat(dataFlow.getId()).isEqualTo(request.getProcessId());
            assertThat(dataFlow.getSource()).isSameAs(request.getSourceDataAddress());
            assertThat(dataFlow.getDestination()).isSameAs(request.getDestinationDataAddress());
            assertThat(dataFlow.getCallbackAddress()).isEqualTo(URI.create("http://any"));
            assertThat(dataFlow.getProperties()).containsKeys("key", "agreementId", "assetId", "participantId");
            assertThat(dataFlow.getState()).isEqualTo(RECEIVED.code());

            verifyNoInteractions(endpointDataReferenceServiceRegistry);
        }

        @Test
        void shouldInitiatePullDataFlow() {
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            var request = dataFlowStartMessageBuilder().flowType(PULL).build();

            when(endpointDataReferenceServiceRegistry.create(any(), any())).thenReturn(ServiceResult.success(dataAddress));

            var result = manager.start(request);

            assertThat(result).isSucceeded().extracting(DataFlowResponseMessage::getDataAddress).isEqualTo(dataAddress);

            var captor = ArgumentCaptor.forClass(DataFlow.class);
            verify(store).save(captor.capture());
            var dataFlow = captor.getValue();
            assertThat(dataFlow.getId()).isEqualTo(request.getProcessId());
            assertThat(dataFlow.getSource()).isSameAs(request.getSourceDataAddress());
            assertThat(dataFlow.getDestination()).isSameAs(request.getDestinationDataAddress());
            assertThat(dataFlow.getCallbackAddress()).isEqualTo(URI.create("http://any"));
            assertThat(dataFlow.getProperties()).containsKeys("key", "agreementId", "assetId", "participantId");
            assertThat(dataFlow.getState()).isEqualTo(STARTED.code());
        }

        @Test
        void shouldNotInitiatePullDataFlow_whenEdrCreationFails() {
            var request = dataFlowStartMessageBuilder().flowType(PULL).build();
            when(endpointDataReferenceServiceRegistry.create(any(), any())).thenReturn(ServiceResult.unexpected("failure"));

            var result = manager.start(request);

            assertThat(result).isFailed().detail().contains("failure");
            verify(store, never()).save(any());
        }
    }

    @Nested
    class StartExistingFlow {
        @Test
        void shouldStartFlow() {
            var request = dataFlowStartMessageBuilder().flowType(PUSH).build();
            var existingFlow = DataFlow.Builder.newInstance().transferType(new TransferType("any", PUSH)).build();
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(existingFlow));

            var result = manager.start(request);

            assertThat(result).isSucceeded();
            var captor = ArgumentCaptor.forClass(DataFlow.class);
            verify(store).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existingFlow).satisfies(stored -> {
                assertThat(stored.getState()).isEqualTo(RECEIVED.code());
            });
        }

        @Test
        void shouldNotStartFlow_whenProvisioningStillOngoing() {
            var request = dataFlowStartMessageBuilder().flowType(PUSH).build();
            var existingFlow = DataFlow.Builder.newInstance().transferType(new TransferType("any", PUSH)).build();
            existingFlow.addResourceDefinitions(List.of(ProvisionResource.Builder.newInstance().flowId(existingFlow.getId()).build()));
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(existingFlow));

            var result = manager.start(request);

            assertThat(result).isFailed();
            verify(store, never()).save(any());
        }

        @Test
        void shouldNotStartFlow_whenCannotLease() {
            var request = dataFlowStartMessageBuilder().flowType(PUSH).build();
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = manager.start(request);

            assertThat(result).isFailed();
            verify(store, never()).save(any());
        }
    }

    @Nested
    class Provision {
        @Test
        void shouldTransitionToProvisioning_whenResourceDefinitionsAreGenerated() {
            var newDestination = DataAddress.Builder.newInstance().type("type").build();
            var request = DataFlowProvisionMessage.Builder.newInstance()
                    .processId("1")
                    .destination(newDestination)
                    .build();
            var definition = ProvisionResource.Builder.newInstance().flowId("any").build();
            when(resourceDefinitionGeneratorManager.generateConsumerResourceDefinition(any())).thenReturn(List.of(definition));

            var result = manager.provision(request);

            assertThat(result).isSucceeded().satisfies(it -> assertThat(it.isProvisioning()).isTrue());
            var captor = ArgumentCaptor.forClass(DataFlow.class);
            verify(store).save(captor.capture());
            var storedDataFlow = captor.getValue();
            assertThat(storedDataFlow.getState()).isEqualTo(PROVISIONING.code());
            assertThat(storedDataFlow.getResourceDefinitions()).containsOnly(definition);
        }

        @Test
        void shouldTransitionToNotified_whenNoResourceDefinitionsGenerated() {
            var newDestination = DataAddress.Builder.newInstance().type("type").build();
            var request = DataFlowProvisionMessage.Builder.newInstance()
                    .processId("1")
                    .destination(newDestination)
                    .build();
            when(resourceDefinitionGeneratorManager.generateConsumerResourceDefinition(any())).thenReturn(emptyList());

            var result = manager.provision(request);

            assertThat(result).isSucceeded().satisfies(it -> assertThat(it.isProvisioning()).isFalse());
            var captor = ArgumentCaptor.forClass(DataFlow.class);
            verify(store).save(captor.capture());
            var storedDataFlow = captor.getValue();
            assertThat(storedDataFlow.getState()).isEqualTo(NOTIFIED.code());
            assertThat(storedDataFlow.getResourceDefinitions()).isEmpty();
        }
    }

    @Nested
    class Provisioning {

        @Test
        void shouldProvisionResourcesToBeProvisioned() {
            var resourceToBeProvisioned = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.CREATED.code()).flowId("any").build();
            var resourceAlreadyProvisioned = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.PROVISIONED.code()).flowId("any").build();
            var newDataAddress = DataAddress.Builder.newInstance().type("any").build();
            var provisionedResource = ProvisionedResource.Builder.from(resourceToBeProvisioned).dataAddress(newDataAddress).build();
            var dataFlow = dataFlowBuilder().state(PROVISIONING.code()).resourceDefinitions(List.of(resourceToBeProvisioned, resourceAlreadyProvisioned)).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.provision(any())).thenReturn(completedFuture(List.of(StatusResult.success(provisionedResource))));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).provision(List.of(resourceToBeProvisioned));
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISION_NOTIFYING.name());
                assertThat(storedDataFlow.getResourceDefinitions()).allMatch(it -> it.getState() == ProvisionResourceStates.PROVISIONED.code());
            });
        }

        @Test
        void shouldTransitionToProvisioningRequested_whenPendingResourcesAreWaitingForProvisioning() {
            var resourceToBeProvisioned = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.CREATED.code()).flowId("any").build();
            var provisionedResource = ProvisionedResource.Builder.from(resourceToBeProvisioned).pending(true).build();
            var dataFlow = dataFlowBuilder().state(PROVISIONING.code()).resourceDefinitions(List.of(resourceToBeProvisioned)).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.provision(any())).thenReturn(completedFuture(List.of(StatusResult.success(provisionedResource))));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).provision(List.of(resourceToBeProvisioned));
                verifyNoInteractions(transferProcessApiClient);
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISION_REQUESTED.name());
                assertThat(storedDataFlow.getResourceDefinitions()).allMatch(it -> it.getState() == ProvisionResourceStates.PROVISION_REQUESTED.code());
            });
        }

        @Test
        void shouldTransitionToProvisioning_whenThereAreStillResourcesToBeProvisioned() {
            var resourceDefinition = ProvisionResource.Builder.newInstance().flowId("any").build();
            var dataFlow = dataFlowBuilder().state(PROVISIONING.code()).resourceDefinitions(List.of(resourceDefinition)).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.provision(any())).thenReturn(completedFuture(List.of(failure(ERROR_RETRY, "error in provisioning"))));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).provision(List.of(resourceDefinition));
                verifyNoInteractions(transferProcessApiClient);
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISIONING.name());
                assertThat(storedDataFlow.getErrorDetail()).contains("error in provisioning");
            });
        }

        @Test
        void shouldTransitionToProvisioning_whenError() {
            var dataFlow = dataFlowBuilder().state(PROVISIONING.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.provision(any())).thenReturn(failedFuture(new EdcException("generic error")));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISIONING.name());
            });
        }

        @Test
        void shouldTransitionToFailed_whenRetryExpired() {
            var dataFlow = dataFlowBuilder().state(PROVISIONING.code()).stateCount(RETRY_EXHAUSTED).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.provision(any())).thenReturn(failedFuture(new EdcException("generic error")));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(FAILED.name());
            });
        }

    }

    @Nested
    class ProvisionNotifying {
        @Test
        void shouldNotifyProvisioningToTransferProcess() {
            var dataFlow = dataFlowBuilder().state(PROVISION_NOTIFYING.code()).stateCount(RETRY_EXHAUSTED).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISION_NOTIFYING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.provisioned(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISIONED.name());
            });
        }

        @Test
        void shouldTransitionToProvisionNotifying_whenError() {
            var dataFlow = dataFlowBuilder().state(PROVISION_NOTIFYING.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISION_NOTIFYING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.provisioned(any())).thenReturn(StatusResult.failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(PROVISION_NOTIFYING.name());
            });
        }

        @Test
        void shouldTransitionToFailed_whenRetryExpired() {
            var dataFlow = dataFlowBuilder().state(PROVISION_NOTIFYING.code()).stateCount(RETRY_EXHAUSTED).build();
            when(store.nextNotLeased(anyInt(), stateIs(PROVISION_NOTIFYING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.provisioned(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(FAILED.name());
            });
        }
    }

    @Nested
    class ResourceProvisioned {
        @Test
        void shouldTransitionToProvisionNotifying_whenProvisionIsComplete() {
            var provisionResource = ProvisionResource.Builder.newInstance().flowId("any").build();
            var dataFlow = dataFlowBuilder().resourceDefinitions(List.of(provisionResource)).build();
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(dataFlow));

            var result = manager.resourceProvisioned(ProvisionedResource.Builder.from(provisionResource).build());

            assertThat(result).isSucceeded();
            verify(store).save(argThat(saved -> saved.getState() == PROVISION_NOTIFYING.code()));
        }

        @Test
        void shouldFail_whenFindByFails() {
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = manager.resourceProvisioned(ProvisionedResource.Builder.newInstance().flowId("any").build());

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        }

    }

    @Nested
    class ResourceDeprovisioned {
        @Test
        void shouldTransitionToDeprovision_whenDeprovisionIsComplete() {
            var provisionResource = ProvisionResource.Builder.newInstance().flowId("any").build();
            var dataFlow = dataFlowBuilder().resourceDefinitions(List.of(provisionResource)).build();
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(dataFlow));

            var result = manager.resourceDeprovisioned(DeprovisionedResource.Builder.from(provisionResource).build());

            assertThat(result).isSucceeded();
            verify(store).save(argThat(saved -> saved.getState() == DEPROVISIONED.code()));
        }

        @Test
        void shouldFail_whenFindByFails() {
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = manager.resourceDeprovisioned(DeprovisionedResource.Builder.newInstance().flowId("any").build());

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        }
    }

    @Nested
    class Terminate {

        @Test
        void shouldTerminateDataFlow() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.terminate("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == TERMINATED.code()));
            verify(transferService).terminate(dataFlow);
        }

        @Test
        void shouldTerminatePullDataFlow() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).id("dataFlowId").transferType(new TransferType("DestinationType", PULL)).build();
            when(store.findByIdAndLease(dataFlow.getId())).thenReturn(StoreResult.success(dataFlow));
            when(endpointDataReferenceServiceRegistry.revoke(dataFlow, null)).thenReturn(ServiceResult.success());

            var result = manager.terminate(dataFlow.getId(), null);

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == TERMINATED.code()));
            verify(endpointDataReferenceServiceRegistry).revoke(dataFlow, null);
        }

        @Test
        void shouldTerminatePullDataFlow_whenSuspendedAndRevokeNotFound() {
            var dataFlow = dataFlowBuilder().state(SUSPENDED.code()).id("dataFlowId").transferType(new TransferType("DestinationType", PULL)).build();
            when(store.findByIdAndLease(dataFlow.getId())).thenReturn(StoreResult.success(dataFlow));
            when(endpointDataReferenceServiceRegistry.revoke(dataFlow, null)).thenReturn(ServiceResult.notFound("not found"));

            var result = manager.terminate(dataFlow.getId(), null);

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == TERMINATED.code()));
            verify(endpointDataReferenceServiceRegistry).revoke(dataFlow, null);
        }

        @Test
        void shouldFailToTerminatePullDataFlow_whenRevocationFails() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).id("dataFlowId").transferType(new TransferType("DestinationType", PULL)).build();
            when(store.findByIdAndLease(dataFlow.getId())).thenReturn(StoreResult.success(dataFlow));
            when(endpointDataReferenceServiceRegistry.revoke(dataFlow, null)).thenReturn(ServiceResult.notFound("failure"));

            var result = manager.terminate(dataFlow.getId(), null);

            assertThat(result).isFailed();
            verify(store, never()).save(any());
            verify(endpointDataReferenceServiceRegistry).revoke(dataFlow, null);
        }

        @Test
        void shouldTerminateDataFlow_withReason() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.terminate("dataFlowId", "test-reason");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == TERMINATED.code() && d.getProperties().get(TERMINATION_REASON).equals("test-reason")));
            verify(transferService).terminate(dataFlow);
        }

        @Test
        void shouldReturnFatalError_whenDataFlowDoesNotExist() {
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.notFound("not found"));

            var result = manager.terminate("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnRetryError_whenEntityCannotBeLeased() {
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = manager.terminate("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(ERROR_RETRY);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnFatalError_whenTransferServiceNotFound() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(null);

            var result = manager.terminate("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnFatalError_whenDataFlowCannotBeTerminated() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.error("cannot be terminated"));

            var result = manager.terminate("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
        }

        @Test
        void shouldStillTerminate_whenDataFlowHasNoSource() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.notFound());

            var result = manager.terminate("dataFlowId", "test-reason");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(f -> f.getProperties().containsKey(TERMINATION_REASON)));
        }

        @Test
        void shouldTransitionToDeprovisioning_whenFlowIsProvisionedOnConsumerSide() {
            var dataFlow = dataFlowBuilder().state(PROVISIONED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.terminate("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == DEPROVISIONING.code()));
            verifyNoInteractions(transferService, endpointDataReferenceServiceRegistry);
        }

        @Test
        void shouldTransitionToDeprovisioning_whenFlowHasResourcesToBeDeprovisioned() {
            var provisionResource = ProvisionResource.Builder.newInstance().flowId("dataFlowId").build();
            provisionResource.transitionProvisioned(ProvisionedResource.Builder.from(provisionResource).build());
            var dataFlow = dataFlowBuilder().state(STARTED.code()).resourceDefinitions(List.of(provisionResource)).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.terminate("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == DEPROVISIONING.code()));
            verify(transferService).terminate(dataFlow);
        }

    }

    @Nested
    class Received {

        @Test
        void shouldStartTransferAndTransitionToStarted_whenValidationSucceeds() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.success());
            when(transferService.transfer(any())).thenReturn(new CompletableFuture<>());

            manager.start();

            await().untilAsserted(() -> {
                verify(transferService).transfer(isA(DataFlowStartMessage.class));
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.getState()).isEqualTo(STARTED.code());
                assertThat(storedDataFlow.getRuntimeId()).isEqualTo(runtimeId);
            });
        }

        @Test
        void shouldTransitionToCompleted_whenTransferSucceeds() {
            var receivedDataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            var startedDataFlow = dataFlowBuilder().state(STARTED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(receivedDataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(startedDataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.success());
            var transferFuture = new CompletableFuture<StreamResult<Object>>();
            when(transferService.transfer(any())).thenReturn(transferFuture);

            manager.start();

            transferFuture.complete(StreamResult.success());

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == COMPLETED.code()));
            });
        }

        @Test
        void shouldNotTransitionToCompleted_whenTransferIsNotStartedAtCompletion() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            var terminatedDataFlow = dataFlowBuilder().state(TERMINATED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(terminatedDataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.success());
            var transferFuture = new CompletableFuture<StreamResult<Object>>();
            when(transferService.transfer(any())).thenReturn(transferFuture);

            manager.start();

            transferFuture.complete(StreamResult.success());

            await().untilAsserted(() -> {
                verify(store, never()).save(argThat(it -> it.getState() == COMPLETED.code()));
                verify(store).save(argThat(it -> it.getState() == TERMINATED.code())); // break lease
            });
        }

        @Test
        void shouldTransitionToFailed_whenTransferFails() {
            var receivedDataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            var startedDataFlow = dataFlowBuilder().state(STARTED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(receivedDataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(startedDataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.success());
            var transferFuture = new CompletableFuture<StreamResult<Object>>();
            when(transferService.transfer(any())).thenReturn(transferFuture);

            manager.start();

            transferFuture.complete(StreamResult.error("an error"));

            await().untilAsserted(() -> {
                verify(transferService).transfer(isA(DataFlowStartMessage.class));
                verify(store, atLeastOnce()).save(argThat(it -> it.getState() == FAILED.code() && it.getErrorDetail().equals(GENERAL_ERROR + ": an error")));
            });
        }

        @Test
        void shouldTransitionToFailed_whenTransferFutureFails() {
            var receivedDataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            var startedDataFlow = dataFlowBuilder().state(STARTED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(receivedDataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(startedDataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.success());
            var transferFuture = new CompletableFuture<StreamResult<Object>>();
            when(transferService.transfer(any())).thenReturn(transferFuture);

            manager.start();

            transferFuture.obtrudeException(new RuntimeException("an error"));

            await().untilAsserted(() -> {
                verify(transferService).transfer(isA(DataFlowStartMessage.class));
                verify(store, atLeastOnce()).save(argThat(it -> it.getState() == FAILED.code() && it.getErrorDetail().contains("an error")));
            });
        }

        @Test
        void shouldStartTransferAndTransitionToReceivedForRetrying_whenValidationFails() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.canHandle(any())).thenReturn(true);
            when(transferService.validate(any())).thenReturn(Result.failure("an error"));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferService, never()).transfer(isA(DataFlowStartMessage.class));
                verify(store, atLeastOnce()).save(argThat(it -> it.getState() == RECEIVED.code()));
            });
        }

        @Test
        void shouldTransitToFailedIfNoTransferServiceCanHandleStarted() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(RECEIVED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(null);

            manager.start();

            await().untilAsserted(() -> {
                verifyNoInteractions(transferService);
                verify(store, atLeastOnce()).save(argThat(it -> it.getState() == FAILED.code()));
            });
        }
    }

    @Nested
    class Completed {
        @Test
        void shouldNotifyResultToControlPlaneAndTriggerDeprovisioning_whenThereAreProvisionResources() {
            var resource = ProvisionResource.Builder.newInstance().flowId("flowId").build();
            resource.transitionProvisioned(ProvisionedResource.Builder.from(resource).build());
            var dataFlow = dataFlowBuilder().state(COMPLETED.code())
                    .resourceDefinitions(List.of(resource))
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(COMPLETED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.completed(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).completed(any());
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                assertThat(captor.getValue()).extracting(StatefulEntity::getState).isEqualTo(DEPROVISIONING.code());
            });
        }

        @Test
        void shouldNotifyResultToControlPlaneAndTransitionToNotify_whenThereAreNoProvisionResources() {
            var dataFlow = dataFlowBuilder().state(COMPLETED.code())
                    .resourceDefinitions(emptyList())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(COMPLETED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.completed(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).completed(any());
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                assertThat(captor.getValue()).extracting(StatefulEntity::getState).isEqualTo(NOTIFIED.code());
            });
        }

        @Test
        void shouldTransitionToCompleted_whenRetriableError() {
            var dataFlow = dataFlowBuilder().state(COMPLETED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(COMPLETED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.completed(any())).thenReturn(failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).completed(any());
                verify(store).save(argThat(it -> it.getState() == COMPLETED.code()));
            });
        }

        @Test
        void shouldTransitionToTerminated_whenFatalError() {
            var dataFlow = dataFlowBuilder().state(COMPLETED.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(COMPLETED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(transferProcessApiClient.completed(any())).thenReturn(failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).completed(any());
                verify(store).save(argThat(it -> it.getState() == TERMINATED.code()));
            });
        }
    }

    @Nested
    class Failed {
        @Test
        void shouldNotifyResultToControlPlane() {
            var dataFlow = dataFlowBuilder().state(FAILED.code()).errorDetail("an error").build();
            when(store.nextNotLeased(anyInt(), stateIs(FAILED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findById(any())).thenReturn(dataFlow);

            when(transferProcessApiClient.failed(any(), eq("an error"))).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).failed(any(), eq("an error"));
                verify(store).save(argThat(it -> it.getState() == NOTIFIED.code()));
            });
        }

        @Test
        void shouldTransitionToFailed_whenRetryableError() {
            var dataFlow = dataFlowBuilder().state(FAILED.code()).errorDetail("an error").build();
            when(store.nextNotLeased(anyInt(), stateIs(FAILED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findById(any())).thenReturn(dataFlow);

            when(transferProcessApiClient.failed(any(), eq("an error"))).thenReturn(failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).failed(any(), eq("an error"));
                verify(store).save(argThat(it -> it.getState() == FAILED.code()));
            });
        }

        @Test
        void shouldTransitionToTerminated_whenFatalError() {
            var dataFlow = dataFlowBuilder().state(FAILED.code()).errorDetail("an error").build();
            when(store.nextNotLeased(anyInt(), stateIs(FAILED.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(store.findById(any())).thenReturn(dataFlow);

            when(transferProcessApiClient.failed(any(), eq("an error"))).thenReturn(failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessApiClient).failed(any(), eq("an error"));
                verify(store).save(argThat(it -> it.getState() == TERMINATED.code()));
            });
        }
    }

    @Nested
    class Deprovisioning {

        @Test
        void shouldDeprovisionResourcesToBeProvisioned() {
            var toBeDeprovisionedResource = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.PROVISIONED.code()).flowId("any").build();
            var alreadyDeprovisionedResource = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.DEPROVISIONED.code()).flowId("any").build();
            var deprovisionedResource = DeprovisionedResource.Builder.from(toBeDeprovisionedResource).build();
            var dataFlow = dataFlowBuilder().state(DEPROVISIONING.code()).resourceDefinitions(List.of(toBeDeprovisionedResource, alreadyDeprovisionedResource)).build();
            when(store.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.deprovision(any())).thenReturn(completedFuture(List.of(StatusResult.success(deprovisionedResource))));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).deprovision(List.of(toBeDeprovisionedResource));
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(DEPROVISIONED.name());
                assertThat(storedDataFlow.getResourceDefinitions()).allMatch(it -> it.getState() == ProvisionResourceStates.DEPROVISIONED.code());
            });
        }

        @Test
        void shouldTransitionToDeprovisioningRequested_whenPendingResourcesAreWaitingForDeprovisioning() {
            var resourceToBeDeprovisioned = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.PROVISIONED.code()).flowId("any").build();
            var deprovisionedResource = DeprovisionedResource.Builder.from(resourceToBeDeprovisioned).pending(true).build();
            var dataFlow = dataFlowBuilder().state(DEPROVISIONING.code()).resourceDefinitions(List.of(resourceToBeDeprovisioned)).build();
            when(store.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.deprovision(any())).thenReturn(completedFuture(List.of(StatusResult.success(deprovisionedResource))));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).deprovision(List.of(resourceToBeDeprovisioned));
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(DEPROVISION_REQUESTED.name());
                assertThat(storedDataFlow.getResourceDefinitions()).allMatch(it -> it.getState() == ProvisionResourceStates.DEPROVISION_REQUESTED.code());
            });
        }

        @Test
        void shouldTransitionToDeprovisioning_whenThereAreStillResourcesToBeDeprovisioned() {
            var resourceDefinition = ProvisionResource.Builder.newInstance().state(ProvisionResourceStates.PROVISIONED.code()).flowId("any").build();
            var dataFlow = dataFlowBuilder().state(DEPROVISIONING.code()).resourceDefinitions(List.of(resourceDefinition)).build();
            when(store.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.deprovision(any())).thenReturn(completedFuture(List.of(
                    failure(ERROR_RETRY, "deprovision failure")
            )));

            manager.start();

            await().untilAsserted(() -> {
                verify(provisionerManager).deprovision(List.of(resourceDefinition));
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(DEPROVISIONING.name());
                assertThat(storedDataFlow.getErrorDetail()).contains("deprovision failure");
            });
        }

        @Test
        void shouldTransitionToDeprovisioning_whenError() {
            var dataFlow = dataFlowBuilder().state(DEPROVISIONING.code()).build();
            when(store.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.deprovision(any())).thenReturn(failedFuture(new EdcException("generic error")));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(DEPROVISIONING.name());
            });
        }

        @Test
        void shouldTransitionToDeprovisionFailed_whenRetryExpired() {
            var dataFlow = dataFlowBuilder().state(DEPROVISIONING.code()).stateCount(RETRY_EXHAUSTED).build();
            when(store.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(provisionerManager.deprovision(any())).thenReturn(failedFuture(new EdcException("generic error")));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.stateAsString()).isEqualTo(DEPROVISION_FAILED.name());
            });
        }

    }

    @Nested
    class Suspend {

        @Test
        void shouldSuspendDataFlow() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.suspend("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == SUSPENDED.code()));
            verify(transferService).terminate(dataFlow);
        }

        @Test
        void shouldSuspendDataFlow_withReason() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.success());

            var result = manager.suspend("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(d -> d.getState() == SUSPENDED.code()));
            verify(transferService).terminate(dataFlow);
        }

        @Test
        void shouldReturnFatalError_whenDataFlowDoesNotExist() {
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.notFound("not found"));

            var result = manager.suspend("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnRetryError_whenEntityCannotBeLeased() {
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = manager.suspend("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(ERROR_RETRY);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnFatalError_whenTransferServiceNotFound() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(null);

            var result = manager.suspend("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
            verifyNoInteractions(transferService);
        }

        @Test
        void shouldReturnFatalError_whenDataFlowCannotBeSuspended() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.error("cannot be suspended"));

            var result = manager.suspend("dataFlowId");

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            verify(store, never()).save(any());
        }

        @Test
        void shouldStillSuspend_whenDataFlowHasNoSource() {
            var dataFlow = dataFlowBuilder().state(RECEIVED.code()).build();
            when(store.findByIdAndLease("dataFlowId")).thenReturn(StoreResult.success(dataFlow));
            when(registry.resolveTransferService(any())).thenReturn(transferService);
            when(transferService.terminate(any())).thenReturn(StreamResult.notFound());

            var result = manager.suspend("dataFlowId");

            assertThat(result).isSucceeded();
            verify(store).save(argThat(f -> f.getState() == SUSPENDED.code()));
        }
    }

    @Nested
    class RestartFlows {

        @Test
        void shouldRestartFlows() {
            var dataFlow = dataFlowBuilder().state(STARTED.code()).transferType(new TransferType("any", PUSH)).build();
            var anotherDataFlow = dataFlowBuilder().state(STARTED.code()).transferType(new TransferType("any", PUSH)).build();
            when(store.nextNotLeased(anyInt(), any(Criterion[].class)))
                    .thenReturn(List.of(dataFlow)).thenReturn(List.of(anotherDataFlow)).thenReturn(emptyList());

            var result = manager.restartFlows();

            assertThat(result).isSucceeded();
            await().untilAsserted(() -> {
                verify(store, times(2)).save(argThat(it -> it.getState() == RECEIVED.code()));
                var captor = ArgumentCaptor.forClass(Criterion[].class);
                verify(store, atLeast(1)).nextNotLeased(anyInt(), captor.capture());
                assertThat(captor.getValue()).contains(new Criterion("transferType.flowType", "=", "PUSH"));
            });
        }
    }

    @Nested
    class UpdateFlowLease {

        @Test
        void shouldUpdateFlow_whenFlowStartedAfterFlowLease() {
            var clock = Clock.systemDefaultZone();
            var updatedAt = clock.millis();
            var dataFlow = dataFlowBuilder().runtimeId(runtimeId).state(STARTED.code()).stateCount(0).updatedAt(updatedAt).clock(clock).build();
            when(store.nextNotLeased(anyInt(), startedFlowOwnedByThisRuntime()))
                    .thenReturn(List.of(dataFlow)).thenReturn(emptyList());

            manager.start();
            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.getState()).isEqualTo(STARTED.code());
                assertThat(storedDataFlow.getRuntimeId()).isEqualTo(runtimeId);
                assertThat(storedDataFlow.getStateCount()).isEqualTo(0);
                assertThat(storedDataFlow.getUpdatedAt()).isGreaterThan(updatedAt);
            });
        }
    }

    @Nested
    class RestartFlowOwnedByAnotherRuntime {
        @Test
        void shouldRestartPushFlow_whenAnotherRuntimeAbandonedIt() {
            var dataFlow = dataFlowBuilder().state(STARTED.code()).transferType(new TransferType("any", PUSH)).build();
            when(store.nextNotLeased(anyInt(), startedFlowOwnedByAnotherRuntime()))
                    .thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(registry.resolveTransferService(any())).thenReturn(transferService);

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.getState()).isEqualTo(RECEIVED.code());
                assertThat(storedDataFlow.getRuntimeId()).isEqualTo(runtimeId);
            });
        }

        @Test
        void shouldRestartPullFlow_whenAnotherRuntimeAbandonedIt() {
            var dataFlow = dataFlowBuilder().state(STARTED.code()).transferType(new TransferType("any", PULL)).build();
            when(store.nextNotLeased(anyInt(), startedFlowOwnedByAnotherRuntime()))
                    .thenReturn(List.of(dataFlow)).thenReturn(emptyList());
            when(registry.resolveTransferService(any())).thenReturn(transferService);

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(DataFlow.class);
                verify(store).save(captor.capture());
                var storedDataFlow = captor.getValue();
                assertThat(storedDataFlow.getState()).isEqualTo(STARTED.code());
                assertThat(storedDataFlow.getRuntimeId()).isEqualTo(runtimeId);
            });
        }
    }

    private DataFlow.Builder dataFlowBuilder() {
        return DataFlow.Builder.newInstance()
                .source(DataAddress.Builder.newInstance().type("source").build())
                .destination(DataAddress.Builder.newInstance().type("destination").build())
                .callbackAddress(URI.create("http://any"))
                .transferType(new TransferType("DestinationType", PUSH))
                .properties(Map.of("key", "value"));
    }

    private DataFlowStartMessage.Builder dataFlowStartMessageBuilder() {
        return DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .callbackAddress(URI.create("http://any"))
                .properties(Map.of("key", "value"));
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{hasState(state)});
    }

    private Criterion[] startedFlowOwnedByThisRuntime() {
        return arrayContains(new Criterion[]{hasState(STARTED.code()), new Criterion("runtimeId", "=", runtimeId)});
    }

    private Criterion[] startedFlowOwnedByAnotherRuntime() {
        return arrayContains(new Criterion[]{hasState(STARTED.code()), new Criterion("runtimeId", "!=", runtimeId)});
    }

}
