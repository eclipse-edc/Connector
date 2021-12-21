/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED_ACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncTransferProcessManagerImplConsumerTest {

    private static final long TIMEOUT = 5;
    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private AsyncTransferProcessManager transferProcessManager;

    @BeforeEach
    void setup() {
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerManifest(any(TransferProcess.class))).thenReturn(resourceManifest);

        transferProcessManager = AsyncTransferProcessManager.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(mock(DataFlowManager.class))
                .waitStrategy(mock(TransferWaitStrategy.class))
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .statusCheckerRegistry(statusCheckerRegistry)
                .build();
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to finite processes")
    void verifyCheckProvisioned_allAreFinite() throws InterruptedException {
        TransferProcess process = createTransferProcess(REQUESTED);
        process.getProvisionedResourceSet().addResource(new TestResource());

        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        when(processStoreMock.find(process.getId())).thenReturn(process);

        transferProcessManager.start(processStoreMock);
        var future = transferProcessManager.transitionRequestAck(process.getId());

        assertThat(future).succeedsWithin(5, SECONDS);
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(IN_PROGRESS.code());
        verify(processStoreMock, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(processStoreMock, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to non-finite processes")
    void verifyCheckProvisioned_allAreNonFinite() throws InterruptedException, ExecutionException, TimeoutException {
        TransferType type = TransferType.Builder.transferType()
                .isFinite(false).build();

        TransferProcess process = createTransferProcess(REQUESTED, type, true);
        process.getProvisionedResourceSet().addResource(new TestResource());

        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        when(processStoreMock.find(process.getId())).thenReturn(process);

        transferProcessManager.start(processStoreMock);
        transferProcessManager.transitionRequestAck(process.getId()).get(5, SECONDS);

        assertThat(process.getState()).describedAs("State should be STREAMING").isEqualTo(TransferProcessStates.STREAMING.code());
        verify(processStoreMock, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkProvisioned: empty provisioned resources")
    void verifyCheckProvisioned_emptyProvisionedResoures() throws InterruptedException {
        TransferProcess process = createTransferProcess(REQUESTED);

        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        when(processStoreMock.find(process.getId())).thenReturn(process);
        when(processStoreMock.nextForState(eq(REQUESTED_ACK.code()), anyInt())).thenReturn(List.of(process));

        transferProcessManager.start(processStoreMock);
        var future = transferProcessManager.transitionRequestAck(process.getId());

        assertThat(future).succeedsWithin(5, SECONDS);
        assertThat(process.getState()).describedAs("State should be REQUESTED_ACK").isEqualTo(REQUESTED_ACK.code());
        verify(processStoreMock, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkComplete: should transition process with managed resources if checker returns completed")
    void verifyCompletedManagedResources() throws InterruptedException, ExecutionException, TimeoutException {
        TransferProcess process = createTransferProcess(REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        when(processStoreMock.nextForState(eq(IN_PROGRESS.code()), anyInt()))
                .thenReturn(List.of(process)).thenReturn(emptyList());

        when(processStoreMock.find(process.getId())).thenReturn(process);

        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(processStoreMock).update(any());

        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);

        transferProcessManager.start(processStoreMock);
        transferProcessManager.complete(process.getId()).get(5, SECONDS);

        assertThat(cdl.await(TIMEOUT, SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(processStoreMock, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkComplete: should transition process with no managed resources if checker returns completed")
    void verifyCompletedNonManagedResources() throws InterruptedException, ExecutionException, TimeoutException {
        //arrange
        TransferProcess process = createTransferProcess(REQUESTED_ACK, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        when(processStoreMock.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process));

        processStoreMock.update(process);
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(processStoreMock).update(process);
        when(processStoreMock.find(process.getId())).thenReturn(process);

        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);

        transferProcessManager.start(processStoreMock);
        transferProcessManager.complete(process.getId()).get(5, SECONDS);

        assertThat(cdl.await(TIMEOUT, SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(processStoreMock, atLeastOnce()).update(process);
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcess(inState, new TransferType(), true);
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type, boolean managed) {
        String processId = UUID.randomUUID().toString();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(processId)
                .transferType(type)
                .managedResources(managed)
                .dataDestination(DataAddress.Builder.newInstance().type("destination-type").build())
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(TransferProcess.Type.CONSUMER)
                .id("test-process-" + processId)
                .state(inState.code())
                .dataRequest(dataRequest)
                .build();
    }

    private static class TestResource extends ProvisionedDataDestinationResource {
        protected TestResource() {
            super();
        }

        @Override
        public DataAddress createDataDestination() {
            return null;
        }

        @Override
        public String getResourceName() {
            return "test-resource";
        }
    }
}
