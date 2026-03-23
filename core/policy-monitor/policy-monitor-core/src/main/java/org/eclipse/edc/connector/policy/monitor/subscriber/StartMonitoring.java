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

import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.policy.monitor.manager.PolicyMonitor;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;

/**
 * Event subscriber that will start monitoring a transfer process whenever it gets started and it's a PROVIDER one.
 */
public class StartMonitoring implements EventSubscriber {

    private final PolicyMonitor policyMonitor;

    public StartMonitoring(PolicyMonitor policyMonitor) {
        this.policyMonitor = policyMonitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof TransferProcessStarted started && PROVIDER.name().equals(started.getType())) {
            policyMonitor.start(started.getTransferProcessId(), started.getContractId());
        }
    }
}
