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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.StatusChecker;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessEvent;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessInitiated;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessRequested;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessStarted;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessTerminated;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.matchers.EventEnvelopeMatcher.isEnvelopeOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        extension.registerServiceMock(DataService.class, mock(DataService.class));
        extension.registerServiceMock(DataPlaneInstanceStore.class, mock(DataPlaneInstanceStore.class));
    }

    @Test
    void shouldDispatchEventsOnTransferProcessStateChanges(TransferProcessService service, TransferProcessProtocolService protocolService,
                                                           EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                           StatusCheckerRegistry statusCheckerRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);
        var statusCheck = mock(StatusChecker.class);

        statusCheckerRegistry.register("any", statusCheck);
        when(statusCheck.isComplete(any(), any())).thenReturn(false);


        var dataRequest = DataRequest.Builder.newInstance()
                .id("dataRequestId")
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(false)
                .connectorAddress("http://an/address")
                .build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .build();

        var initiateResult = service.initiateTransfer(transferRequest);

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessInitiated.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessProvisioned.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessRequested.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessRequested.class)));
        });

        ArgumentCaptor<EventEnvelope<TransferProcessStarted>> captor = ArgumentCaptor.forClass(EventEnvelope.class);

        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var startMessage = TransferStartMessage.Builder.newInstance()
                .processId("dataRequestId")
                .protocol("any")
                .callbackAddress("http://any")
                .dataAddress(dataAddress)
                .build();

        protocolService.notifyStarted(startMessage, ClaimToken.Builder.newInstance().build());

        await().untilAsserted(() -> {
            verify(eventSubscriber, times(4)).on(captor.capture());
            Assertions.assertThat(captor.getValue()).isNotNull()
                    .extracting(EventEnvelope::getPayload)
                    .extracting(TransferProcessStarted::getDataAddress)
                    .usingRecursiveComparison().isEqualTo(dataAddress);
        });

        service.complete(initiateResult.getContent());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessCompleted.class)));
        });

        service.deprovision(initiateResult.getContent());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessDeprovisioned.class)));
        });
    }

    @Test
    void shouldDispatchEventOnTransferProcessTerminated(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);

        var dataRequest = DataRequest.Builder.newInstance()
                .id(String.valueOf(UUID.randomUUID()))
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(true)
                .connectorAddress("http://an/address")
                .build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .build();

        var initiateResult = service.initiateTransfer(transferRequest);

        service.terminate(initiateResult.getContent(), "any reason");

        await().untilAsserted(() -> verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessTerminated.class))));
    }

    @Test
    void shouldDispatchEventOnTransferProcessFailure(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any())).thenReturn(failedFuture(new RuntimeException("an error")));
        dispatcherRegistry.register(testDispatcher);
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);

        var dataRequest = DataRequest.Builder.newInstance()
                .id(String.valueOf(UUID.randomUUID()))
                .assetId("assetId")
                .destinationType("any")
                .protocol("test")
                .managedResources(false)
                .connectorAddress("http://an/address")
                .build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .build();

        service.initiateTransfer(transferRequest);

        await().untilAsserted(() -> verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessTerminated.class))));
    }

}
