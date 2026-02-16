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

package org.eclipse.edc.connector.controlplane.transfer.spi.observe;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe transfer process
 * state changes via {@link Observable#registerListener}.
 * <p>
 * Note that the listener is called before state changes are persisted.
 * Therefore, when using a persistent transfer store implementation, it
 * is guaranteed to be called at least once.
 */
public interface TransferProcessListener {

    /**
     * Called after a {@link TransferProcess} was initiated.
     *
     * @param process the transfer process that has been initiated.
     */
    default void initiated(TransferProcess process) {

    }

    /**
     * Called after an asynchronous preparation for a {@link TransferProcess} was requested.
     *
     * @param process the transfer process that has been requested for preparation.
     */
    default void preparationRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was provisioned.
     *
     * @param process the transfer process that has been provisioned.
     * @deprecated TP doesn't manage provisioning anymore.
     */
    @Deprecated(since = "0.16.0")
    default void provisioned(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was prepared.
     *
     * @param process the transfer process that has been prepared.
     */
    default void prepared(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was requested.
     *
     * @param process the transfer process that has been requested.
     */
    default void requested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} startup was requested.
     *
     * @param process the transfer process that has been requested to start.
     */
    default void startupRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} start was requested.
     *
     * @param process the transfer process that has been requested to start.
     */
    default void startingRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was started.
     *
     * @param process the transfer process that has been started.
     */
    default void started(TransferProcess process, TransferProcessStartedData additionalData) {

    }

    /**
     * Called after a {@link TransferProcess} completing was requested.
     *
     * @param process the transfer process that has been requested to complete.
     */
    default void completingRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was completed.
     *
     * @param process the transfer process that has been completed.
     */
    default void completed(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} termination was requested.
     *
     * @param process the transfer process that has been requested to terminate.
     */
    default void terminatingRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was terminated.
     *
     * @param process the transfer process that has been terminated.
     */
    default void terminated(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was suspended.
     *
     * @param process the transfer process that has been suspended.
     */
    default void suspended(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} suspend was requested.
     *
     * @param process the transfer process that has been requested to suspend.
     */
    default void suspendingRequested(TransferProcess process) {

    }

    /**
     * Called after an asynchronous deprovisioning for a {@link TransferProcess} was requested.
     *
     * @param process the transfer process that has been requested for deprovisioning.
     */
    default void deprovisioningRequested(TransferProcess process) {

    }

    /**
     * Called after a {@link TransferProcess} was deprovisioned.
     *
     * @param process the transfer process that has been deprovisioned.
     */
    default void deprovisioned(TransferProcess process) {

    }

}
