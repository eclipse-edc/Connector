/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.transfer.process;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.TestProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.TestResourceDefinition;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.SecretToken;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.TransferProcessCommand;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_SECRET;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class TransferProcessManagerImplTest {

    private static final String DESTINATION_TYPE = "test-type";
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private static final String PROVISIONED_RESOURCE_ID = "1";
    private static final int RETRY_LIMIT = 1;
    private static final int RETRY_EXHAUSTED = RETRY_LIMIT + 1;

    private final ProvisionManager provisionManager = mock();
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final StatusCheckerRegistry statusCheckerRegistry = mock();
    private final ResourceManifestGenerator manifestGenerator = mock();
    private final TransferProcessStore transferProcessStore = mock();
    private final PolicyArchive policyArchive = mock();
    private final DataFlowManager dataFlowManager = mock();
    private final Vault vault = mock();
    private final Clock clock = Clock.systemUTC();
    private final TransferProcessListener listener = mock();
    private final CommandQueue<TransferProcessCommand> commandQueue = mock();
    private final CommandRunner<TransferProcessCommand> commandRunner = mock();
    private final ProtocolWebhook protocolWebhook = mock();
    private final DataAddressResolver addressResolver = mock();
    private final String protocolWebhookUrl = "http://protocol.webhook/url";

    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        when(protocolWebhook.url()).thenReturn(protocolWebhookUrl);
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success(createDataFlowResponse()));
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L));
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(() -> 50L)
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .typeManager(new TypeManager())
                .clock(clock)
                .statusCheckerRegistry(statusCheckerRegistry)
                .observable(observable)
                .transferProcessStore(transferProcessStore)
                .policyArchive(policyArchive)
                .vault(vault)
                .addressResolver(addressResolver)
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .protocolWebhook(protocolWebhook)
                .build();
    }

    @Test
    void initiateConsumerRequest() {
        when(transferProcessStore.findForCorrelationId("1")).thenReturn(null);
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
        var dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .callbackAddresses(List.of(callback))
                .build();

        var captor = ArgumentCaptor.forClass(TransferProcess.class);

        manager.initiateConsumerRequest(transferRequest);

        verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(captor.capture());
        var transferProcess = captor.getValue();
        assertThat(transferProcess.getId()).isEqualTo("1").isEqualTo(transferProcess.getCorrelationId());
        assertThat(transferProcess.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
        verify(listener).initiated(any());
    }

    @Test
    void initial_consumer_shouldTransitionToProvisioning() {
        var transferProcess = createTransferProcess(INITIAL);
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code())))
                .thenReturn(List.of(transferProcess))
                .thenReturn(emptyList());
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class)))
                .thenReturn(Result.success(resourceManifest));

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verifyNoInteractions(provisionManager);
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == PROVISIONING.code()));
        });
    }

    @Test
    void initial_consumer_manifestEvaluationFailed_shouldTransitionToTerminated() {
        var transferProcess = createTransferProcess(INITIAL);
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code())))
                .thenReturn(List.of(transferProcess))
                .thenReturn(emptyList());
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class)))
                .thenReturn(Result.failure("error"));

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verifyNoInteractions(provisionManager);
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
        });
    }

    @Test
    void initial_consumer_shouldTransitionToTerminated_whenNoPolicyFound() {
        var transferProcess = createTransferProcess(INITIAL);
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code())))
                .thenReturn(List.of(transferProcess))
                .thenReturn(emptyList());
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(null);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verifyNoInteractions(provisionManager);
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
        });
    }

    @Test
    void initial_provider_shouldTransitionToProvisioning() {
        var transferProcess = createTransferProcessBuilder(INITIAL).type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
        var contentDataAddress = DataAddress.Builder.newInstance().type("type").build();
        when(addressResolver.resolveForAsset(any())).thenReturn(contentDataAddress);
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateProviderResourceManifest(any(DataRequest.class), any(), any()))
                .thenReturn(resourceManifest);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).updateOrCreate(captor.capture());
            verify(manifestGenerator).generateProviderResourceManifest(any(), any(), any());
            verifyNoInteractions(provisionManager, vault);
            var actualTransferProcess = captor.getValue();
            assertThat(actualTransferProcess.getState()).isEqualTo(PROVISIONING.code());
            assertThat(actualTransferProcess.getContentDataAddress()).isSameAs(contentDataAddress);
            assertThat(actualTransferProcess.getResourceManifest()).isSameAs(resourceManifest);
        });
    }

    @Test
    void initial_provider_shouldStoreSecret_whenItIsFoundInTheDataAddress() {
        var destinationDataAddress = DataAddress.Builder.newInstance()
                .keyName("keyName")
                .type("type")
                .property(EDC_DATA_ADDRESS_SECRET, "secret")
                .build();
        var dataRequest = createDataRequestBuilder().dataDestination(destinationDataAddress).build();
        var transferProcess = createTransferProcessBuilder(INITIAL).type(PROVIDER).dataRequest(dataRequest).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
        when(addressResolver.resolveForAsset(any())).thenReturn(DataAddress.Builder.newInstance().type("type").build());
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateProviderResourceManifest(any(DataRequest.class), any(), any()))
                .thenReturn(resourceManifest);

        manager.start();

        await().untilAsserted(() -> {
            verify(vault).storeSecret("keyName", "secret");
        });
    }

    @Test
    void initial_provider_shouldTransitionToTerminating_whenAssetIsNotResolved() {
        var transferProcess = createTransferProcessBuilder(INITIAL).type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
        when(addressResolver.resolveForAsset(any())).thenReturn(null);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).updateOrCreate(captor.capture());
            verifyNoInteractions(manifestGenerator, provisionManager);
            var actualTransferProcess = captor.getValue();
            assertThat(actualTransferProcess.getState()).isEqualTo(TERMINATING.code());
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnDataDestination() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance()
                .resource(provisionedDataDestinationResource())
                .build());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == PROVISIONED.code()));
            verify(listener).provisioned(process);
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnContentAddress() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();

        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance()
                .resource(createTestProvisionedContentResource("3"))
                .secretToken(new TestToken())
                .build());

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == PROVISIONED.code()));
            verify(vault).storeSecret(any(), any());
            verify(listener).provisioned(process);
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisioningRequestedOnResponseStarted() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();
        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance().inProcess(true).build());

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == PROVISIONING_REQUESTED.code()));
            verify(listener).provisioningRequested(any());
        });
    }

    @Test
    void provisioning_provider_shouldTransitionToTerminating_whenProvisionErrorAndRetriesExhausted() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .type(PROVIDER)
                .stateCount(2)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void provisioning_consumer_shouldTransitionToTerminating_whenProvisionErrorAndRetriesExhausted() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .type(CONSUMER)
                .stateCount(2)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
        });
    }

    @Test
    void provisioning_provider_shouldTransitionToTerminatingOnFatalProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .type(PROVIDER)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void provisioning_consumer_shouldTransitionToTerminatedOnFatalProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .type(CONSUMER)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
        });
    }

    @Test
    void provisioning_shouldContinueOnRetryProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(ResponseStatus.ERROR_RETRY, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == PROVISIONING.code()));
        });
    }

    @Test
    void provisionedConsumer_shouldTransitionToRequesting() {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(CONSUMER).build();
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == REQUESTING.code()));
        });
    }

    @Test
    void provisionedProvider_shouldTransitionToStarting() {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(PROVIDER).build();
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(PROVISIONED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void requesting_shouldSendMessageAndTransitionToRequested() {
        var process = createTransferProcess(REQUESTING);
        when(dispatcherRegistry.dispatch(eq(Object.class), any())).thenReturn(completedFuture(StatusResult.success("any")));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
        when(vault.resolveSecret(any())).thenReturn(null);

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferRequestMessage.class);
            verify(dispatcherRegistry).dispatch(eq(Object.class), captor.capture());
            verify(transferProcessStore, times(1)).updateOrCreate(argThat(p -> p.getState() == REQUESTED.code()));
            verify(listener).requested(process);
            var requestMessage = captor.getValue();
            assertThat(requestMessage.getCallbackAddress()).isEqualTo(protocolWebhookUrl);
            assertThat(requestMessage.getDataDestination().getProperty(EDC_DATA_ADDRESS_SECRET)).isNull();
        });
    }

    @Test
    void requesting_shouldAddSecretToDataAddress_whenItExists() {
        var destination = DataAddress.Builder.newInstance().type("any").keyName("keyName").build();
        var process = createTransferProcessBuilder(REQUESTING).dataRequest(createDataRequestBuilder().dataDestination(destination).build()).build();
        when(dispatcherRegistry.dispatch(eq(Object.class), any())).thenReturn(completedFuture(StatusResult.success("any")));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
        when(vault.resolveSecret(any())).thenReturn("secret");

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferRequestMessage.class);
            verify(dispatcherRegistry).dispatch(eq(Object.class), captor.capture());
            verify(transferProcessStore, times(1)).updateOrCreate(argThat(p -> p.getState() == REQUESTED.code()));
            verify(listener).requested(process);
            verify(vault).resolveSecret("keyName");
            var requestMessage = captor.getValue();
            assertThat(requestMessage.getDataDestination().getProperty(EDC_DATA_ADDRESS_SECRET)).isEqualTo("secret");
        });
    }

    @Test
    void starting_shouldStartDataTransferAndSendMessageToConsumer() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        var dataFlowResponse = createDataFlowResponse();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
        when(dispatcherRegistry.dispatch(any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferStartMessage.class);
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(dispatcherRegistry).dispatch(any(), captor.capture());
            assertThat(captor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(dataFlowResponse.getDataAddress());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        });
    }

    @Test
    void starting_onFailureAndRetriesNotExhausted_updatesStateCountForRetry() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void starting_shouldTransitionToTerminatingIfFatalFailure() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(FATAL_ERROR));

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void starting_onFailureAndRetriesExhausted_transitToTerminating() {
        var process = createTransferProcessBuilder(STARTING).type(PROVIDER).stateCount(RETRY_EXHAUSTED).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void starting_whenShouldWait_updatesStateCount() {
        var process = createTransferProcessBuilder(STARTING).type(PROVIDER).stateCount(2).stateTimestamp(clock.millis() + 1000L).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dataFlowManager);
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void started_shouldComplete_whenCheckerCompleted() {
        var process = createTransferProcessBuilder(STARTED)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(provisionedDataDestinationResource(), provisionedDataDestinationResource()))
                        .build())
                .build();

        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((tp, resources) -> true);

        manager.start();

        await().untilAsserted(() -> {
            verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == COMPLETING.code()));
        });
    }

    @Test
    void started_shouldBreakLeaseAndNotComplete_whenNotAllYetCompleted() {
        var process = createTransferProcess(STARTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((tp, resources) -> false);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == STARTED.code()));
        });
    }

    @Test
    void started_shouldNotComplete_whenNoChecker() {
        var process = createTransferProcess(STARTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(STARTED.code()))).thenReturn(List.of(process));
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).updateOrCreate(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, never()).updateOrCreate(any());
        });
    }

    @Test
    void completing_shouldTransitionToCompleted_whenSendingMessageSucceed() {
        var process = createTransferProcess(COMPLETING);
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(COMPLETING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING.code()).build());
        when(dispatcherRegistry.dispatch(any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).dispatch(any(), isA(TransferCompletionMessage.class));
            verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(argThat(p -> p.getState() == COMPLETED.code()));
            verify(listener).completed(process);
        });
    }

    @Test
    void terminating_shouldTransitionToTerminated_whenMessageSentCorrectly() {
        var process = createTransferProcessBuilder(TERMINATING).type(PROVIDER).build();
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());
        when(dispatcherRegistry.dispatch(any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).dispatch(any(), isA(TransferTerminationMessage.class));
            verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
        });
    }

    @Test
    void terminating_shouldTransitionToTerminatedWithoutSendingMessage_whenConsumerAndIsNotRequestedYet() {
        var process = createTransferProcessBuilder(TERMINATING).type(CONSUMER).state(REQUESTING.code()).build();
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dispatcherRegistry);
            verify(transferProcessStore, times(RETRY_LIMIT)).updateOrCreate(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisioned() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TokenTestProvisionResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.success(DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId(PROVISIONED_RESOURCE_ID)
                .build());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).findById(process.getId());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == DEPROVISIONED.code()));
            verify(vault).deleteSecret(any());
            verify(listener).deprovisioned(process);
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisioningRequestedOnResponseStarted() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TokenTestProvisionResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionedResponse = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("any")
                .inProcess(true)
                .build();

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(StatusResult.success(deprovisionedResponse))));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == DEPROVISIONING_REQUESTED.code()));
            verify(listener).deprovisioningRequested(any());
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisionedWithErrorOnFatalDeprovisionError() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.<DeprovisionedResource>failure(FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == DEPROVISIONED.code() && p.getErrorDetail() != null));
            verify(listener).deprovisioned(process);
        });
    }

    @Test
    void deprovisioning_shouldNotTransitionOnRetriableDeprovisionError() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.<DeprovisionedResource>failure(ResponseStatus.ERROR_RETRY, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == DEPROVISIONING.code()));
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisionedWithErrorOnDeprovisionFailure() {
        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .stateCount(RETRY_EXHAUSTED)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(DEPROVISIONING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.findById(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).updateOrCreate(argThat(p -> p.getState() == DEPROVISIONED.code() && p.getErrorDetail() != null));
            verify(listener).deprovisioned(process);
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DispatchFailureArguments.class)
    void dispatchFailure(TransferProcessStates starting, TransferProcessStates ending, CompletableFuture<StatusResult<Object>> result, UnaryOperator<TransferProcess.Builder> builderEnricher) {
        var negotiation = builderEnricher.apply(createTransferProcessBuilder(starting).state(starting.code())).build();
        when(transferProcessStore.nextNotLeased(anyInt(), stateIs(starting.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(result);
        when(transferProcessStore.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).updateOrCreate(captor.capture());
            assertThat(captor.getAllValues()).hasSize(1).first().satisfies(n -> {
                assertThat(n.getState()).isEqualTo(ending.code());
            });
            verify(dispatcherRegistry, only()).dispatch(any(), any());
        });
    }

    @Test
    void enqueueCommand_willEnqueueCommandOnCommandQueue() {
        var command = new TransferProcessCommand() {
        };

        manager.enqueueCommand(command);

        verify(commandQueue).enqueue(command);
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{ hasState(state) });
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

    private DataFlowResponse createDataFlowResponse() {
        return DataFlowResponse.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .build();
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcessBuilder(inState).build();
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState) {
        var processId = UUID.randomUUID().toString();
        var dataRequest = createDataRequestBuilder()
                .processId(processId)
                .protocol("protocol")
                .connectorAddress("http://an/address")
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(CONSUMER)
                .id("test-process-" + processId)
                .state(inState.code())
                .dataRequest(dataRequest);
    }

    private DataRequest.Builder createDataRequestBuilder() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .destinationType(DESTINATION_TYPE);
    }

    private ProvisionedDataDestinationResource provisionedDataDestinationResource() {
        return new TestProvisionedDataDestinationResource("test-resource", PROVISIONED_RESOURCE_ID);
    }

    private static class DispatchFailureArguments implements ArgumentsProvider {

        private static final int RETRIES_NOT_EXHAUSTED = RETRY_LIMIT;
        private static final int RETRIES_EXHAUSTED = RETRIES_NOT_EXHAUSTED + 1;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // retries not exhausted
                    new DispatchFailure(REQUESTING, REQUESTING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(STARTING, STARTING, failedFuture(new EdcException("error")), b -> b.type(PROVIDER).stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, COMPLETING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(TERMINATING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    // retries exhausted
                    new DispatchFailure(REQUESTING, TERMINATED, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(STARTING, TERMINATING, failedFuture(new EdcException("error")), b -> b.type(PROVIDER).stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(TERMINATING, TERMINATED, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED)),
                    // fatal error, in this case retry should never be done
                    new DispatchFailure(REQUESTING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(STARTING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.type(PROVIDER).stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(TERMINATING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED))
            );
        }
    }

    @JsonDeserialize(builder = TestProvisionedContentResource.Builder.class)
    private static class TestProvisionedContentResource extends ProvisionedContentResource {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProvisionedContentResource.Builder<TestProvisionedContentResource, Builder> {

            protected Builder() {
                super(new TestProvisionedContentResource());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }

    private static class TokenTestProvisionResource extends TestProvisionedDataDestinationResource {
        TokenTestProvisionResource(String resourceName, String id) {
            super(resourceName, id);
            hasToken = true;
        }
    }

    private static class TestToken implements SecretToken {

        @Override
        public long getExpiration() {
            return 0;
        }

    }

}
