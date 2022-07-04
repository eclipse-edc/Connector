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

package org.eclipse.dataspaceconnector.transfer.core.listener;

import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCancelled;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessEnded;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessFailed;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessInitialized;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessRequested;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.time.Clock;

/**
 * Listener responsible for creating and publishing events regarding TransferProcess state changes
 */
public class EventTransferProcessListener implements TransferProcessListener {
    private final EventRouter eventRouter;
    private final Clock clock;

    public EventTransferProcessListener(EventRouter eventRouter, Clock clock) {
        this.eventRouter = eventRouter;
        this.clock = clock;
    }

    @Override
    public void initialized(TransferProcess process) {
        var event = TransferProcessInitialized.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void provisioned(TransferProcess process) {
        var event = TransferProcessProvisioned.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void requested(TransferProcess process) {
        var event = TransferProcessRequested.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void completed(TransferProcess process) {
        var event = TransferProcessCompleted.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deprovisioned(TransferProcess process) {
        var event = TransferProcessDeprovisioned.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void ended(TransferProcess process) {
        var event = TransferProcessEnded.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void cancelled(TransferProcess process) {
        var event = TransferProcessCancelled.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void failed(TransferProcess process) {
        var event = TransferProcessFailed.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }
}
