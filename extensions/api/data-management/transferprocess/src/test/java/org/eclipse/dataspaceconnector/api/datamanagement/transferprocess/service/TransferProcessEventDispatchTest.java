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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service;

import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCancelled;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessEnded;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessFailed;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessInitialized;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessRequested;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.awaitility.Awaitility.await;
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
        extension.setConfiguration(Map.of("edc.transfer.send.retry.limit", "0"));
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
            verify(eventSubscriber).on(isA(TransferProcessInitialized.class));
            verify(eventSubscriber).on(isA(TransferProcessProvisioned.class));
            verify(eventSubscriber).on(isA(TransferProcessRequested.class));
            verify(eventSubscriber).on(isA(TransferProcessCompleted.class));
        });

        service.deprovision(initiateResult.getContent());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(TransferProcessDeprovisioned.class));
            verify(eventSubscriber).on(isA(TransferProcessEnded.class));
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
                .managedResources(true)
                .build();

        service.initiateTransfer(dataRequest);

        await().untilAsserted(() -> verify(eventSubscriber).on(isA(TransferProcessFailed.class)));
    }
}
