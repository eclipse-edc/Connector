/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.tasks.tck.dsp.recorder.Step;

import java.util.function.Function;

public class TransferProcessGuard extends DelayedActionGuard<TransferProcess> implements TransferProcessPendingGuard {

    public TransferProcessGuard(Function<TransferProcess, Step<TransferProcess>> actionProvider, StateEntityStore<TransferProcess> store) {
        super(actionProvider, store);
    }
}
