/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Masatake Iwasaki (NTT DATA) - fixed failure due to assertion timeout
 *
 */

package org.eclipse.edc.connector.service.transferprocess;

import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessCancelled;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessFailed;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessInitiated;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessRequested;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessTerminated;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class TransferProcessEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        var configuration = Map.of(
                "edc.transfer.send.retry.limit", "0",
                "edc.transfer.send.retry.base-delay.ms", "0",
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api"
        );

        extension.setConfiguration(configuration);
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerServiceMock(EventExecutorServiceContainer.class, new EventExecutorServiceContainer(newSingleThreadExecutor()));
    }

    @Test
    void shouldDispatchEventsOnTransferProcessStateChanges(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(eventSubscriber);

        var dataRequest = DataRequest.Builder.newInstance()
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(false)
                .build();

        var initiateResult = service.initiateTransfer(dataRequest);

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(TransferProcessInitiated.class));
            verify(eventSubscriber).on(isA(TransferProcessProvisioned.class));
            verify(eventSubscriber).on(isA(TransferProcessRequested.class));
            verify(eventSubscriber).on(isA(TransferProcessCompleted.class));
        });

        service.deprovision(initiateResult.getContent());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(TransferProcessDeprovisioned.class));
            verify(eventSubscriber).on(isA(TransferProcessTerminated.class));
        });
    }

    @Test
    void shouldDispatchEventOnTransferProcessCanceled(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(eventSubscriber);

        var dataRequest = DataRequest.Builder.newInstance()
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(true)
                .build();

        var initiateResult = service.initiateTransfer(dataRequest);

        service.cancel(initiateResult.getContent());

        await().untilAsserted(() -> verify(eventSubscriber).on(isA(TransferProcessCancelled.class)));
    }

    @Test
    void shouldDispatchEventOnTransferProcessFailure(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("an error")));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(eventSubscriber);

        var dataRequest = DataRequest.Builder.newInstance()
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(false)
                .build();

        service.initiateTransfer(dataRequest);

        await().untilAsserted(() -> verify(eventSubscriber).on(isA(TransferProcessFailed.class)));
    }
}
