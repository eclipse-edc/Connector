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

package org.eclipse.dataspaceconnector.events.azure;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

class AzureEventGridPublisher implements TransferProcessListener {

    private final Monitor monitor;
    private final EventGridPublisherAsyncClient<EventGridEvent> client;
    private final String eventTypeTransferprocess = "dataspaceconnector/transfer/transferprocess";
    private final String eventTypeMetadata = "dataspaceconnector/metadata/store";
    private final String connectorId;

    public AzureEventGridPublisher(String connectorId, Monitor monitor, EventGridPublisherAsyncClient<EventGridEvent> client) {
        this.connectorId = connectorId;
        this.monitor = monitor;
        this.client = client;
    }

    @Override
    public void created(TransferProcess process) {
        var dto = createTransferProcessDto(process);
        if (process.getType() == TransferProcess.Type.CONSUMER) {
            sendEvent("createdConsumer", eventTypeTransferprocess, dto).subscribe(new LoggingSubscriber<>("Transfer process created"));
        } else {
            sendEvent("createdProvider", eventTypeTransferprocess, dto).subscribe(new LoggingSubscriber<>("Transfer process created"));
        }
    }

    @Override
    public void completed(TransferProcess process) {
        sendEvent("completed", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process completed"));
    }


    @Override
    public void deprovisioned(TransferProcess process) {
        sendEvent("deprovisioned", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process resources deprovisioned"));

    }

    @Override
    public void ended(TransferProcess process) {
        sendEvent("ended", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process ended"));

    }

    @Override
    public void error(TransferProcess process) {
        sendEvent("error", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process errored!"));

    }

    private Mono<Void> sendEvent(String what, String where, Object payload) {
        BinaryData data = BinaryData.fromObject(payload);
        var evt = new EventGridEvent(what, where, data, "0.1");
        return client.sendEvent(evt);
    }

    @NotNull
    private TransferProcessDto createTransferProcessDto(TransferProcess process) {
        return TransferProcessDto.Builder.newInstance()
                .connector(connectorId)
                .state(TransferProcessStates.from(process.getState()))
                .requestId(process.getDataRequest().getId())
                .type(process.getType())
                .build();
    }

    private class LoggingSubscriber<T> extends BaseSubscriber<T> {

        private final String message;

        LoggingSubscriber(String message) {
            this.message = message;
        }

        @Override
        protected void hookOnComplete() {
            monitor.info("AzureEventGrid: " + message);
        }

        @Override
        protected void hookOnError(@NotNull Throwable throwable) {
            monitor.severe("Error during event publishing", throwable);
        }
    }
}
