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

package org.eclipse.edc.connector.transfer.process;

import org.eclipse.edc.connector.defaults.storage.transferprocess.InMemoryTransferProcessStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.TestProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.TestResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.comparable;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ComponentTest
class TransferProcessManagerImplIntegrationTest {

    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore store = new InMemoryTransferProcessStore();
    private TransferProcessManagerImpl transferProcessManager;

    @BeforeEach
    void setup() {
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class))).thenReturn(Result.success(resourceManifest));

        var policyArchive = mock(PolicyArchive.class);
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());

        var monitor = mock(Monitor.class);
        var waitStrategy = mock(ExponentialWaitStrategy.class);
        var clock = Clock.systemUTC();
        transferProcessManager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(mock(DataFlowManager.class))
                .waitStrategy(waitStrategy)
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(mock(RemoteMessageDispatcherRegistry.class))
                .manifestGenerator(manifestGenerator)
                .monitor(monitor)
                .clock(clock)
                .commandQueue(mock(CommandQueue.class))
                .commandRunner(mock(CommandRunner.class))
                .typeManager(new TypeManager())
                .statusCheckerRegistry(mock(StatusCheckerRegistry.class))
                .observable(mock(TransferProcessObservable.class))
                .transferProcessStore(store)
                .policyArchive(policyArchive)
                .addressResolver(mock(DataAddressResolver.class))
                .build();
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() {
        var numProcesses = TRANSFER_MANAGER_BATCHSIZE * 2;
        when(provisionManager.provision(any(), any(Policy.class))).thenAnswer(i -> completedFuture(List.of(
                ProvisionResponse.Builder.newInstance()
                        .resource(new TestProvisionedDataDestinationResource("any", "1"))
                        .build()
        )));

        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").build();
        var processes = IntStream.range(0, numProcesses)
                .mapToObj(i -> provisionedResourceSet())
                .map(resourceSet -> createInitialTransferProcess().resourceManifest(manifest).callbackAddresses(List.of(callback)).provisionedResourceSet(resourceSet).build())
                .peek(store::updateOrCreate)
                .collect(Collectors.toList());

        transferProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(processes).describedAs("All transfer processes state should be greater than INITIAL")
                    .allSatisfy(process -> {
                        var id = process.getId();
                        var storedProcess = store.findById(id);

                        assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore")
                                .isNotNull().extracting(StatefulEntity::getState).asInstanceOf(comparable(Integer.class))
                                .isGreaterThan(INITIAL.code());

                        assertThat(storedProcess.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
                    });
            verify(provisionManager, times(numProcesses)).provision(any(), any());
        });

    }

    private ProvisionedResourceSet provisionedResourceSet() {
        return ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test-resource", "1")))
                .build();
    }

    private TransferProcess.Builder createInitialTransferProcess() {
        var processId = UUID.randomUUID().toString();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(processId)
                .destinationType("test-type")
                .contractId(UUID.randomUUID().toString())
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(TransferProcess.Type.CONSUMER)
                .id("test-process-" + processId)
                .dataRequest(dataRequest);
    }
}

