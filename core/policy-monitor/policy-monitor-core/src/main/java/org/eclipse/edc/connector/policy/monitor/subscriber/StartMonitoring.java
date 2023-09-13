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

package org.eclipse.edc.connector.policy.monitor.subscriber;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;

public class StartMonitoring implements EventSubscriber {

    private final PolicyMonitorManager manager;

    public StartMonitoring(PolicyMonitorManager manager) {
        this.manager = manager;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof TransferProcessStarted started && PROVIDER.name().equals(started.getType())) {
            manager.startMonitoring(started.getTransferProcessId(), started.getContractId());
        }
    }
}
