/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.persistence.StateEntityStore;

import java.util.Set;
import java.util.function.Consumer;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;


/**
 * Contract negotiation guard for TCK testcases.
 */
public class TransferProcessGuard extends DelayedActionGuard<TransferProcess> implements TransferProcessPendingGuard {
    // the states not to apply the guard to - i.e., to allow automatic transitions by the transfer process manager
    private static final Set<Integer> PROVIDER_AUTOMATIC_STATES = Set.of(
            INITIAL.code(),
            STARTING.code(),
            SUSPENDING.code(),
            SUSPENDING_REQUESTED.code(),
            RESUMING.code(),
            COMPLETING.code(),
            COMPLETING_REQUESTED.code(),
            PROVISIONING.code(),
            DEPROVISIONING.code(),
            TERMINATING.code(),
            TERMINATING_REQUESTED.code());

    private static final Set<Integer> CONSUMER_AUTOMATIC_STATES = Set.of(
            INITIAL.code(),
            REQUESTING.code(),
            PROVISIONING.code(),
            DEPROVISIONING.code(),
            SUSPENDING.code(),
            SUSPENDING_REQUESTED.code(),
            COMPLETING.code(),
            COMPLETING_REQUESTED.code(),
            TERMINATING.code(),
            TERMINATING_REQUESTED.code()
    );

    public TransferProcessGuard(Consumer<TransferProcess> action, StateEntityStore<TransferProcess> store) {
        super(cn -> cn.getType() == PROVIDER ?
                !PROVIDER_AUTOMATIC_STATES.contains(cn.getState()) : !CONSUMER_AUTOMATIC_STATES.contains(cn.getState()), action, store);
    }
}
