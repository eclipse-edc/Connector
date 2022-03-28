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

package org.eclipse.dataspaceconnector.spi.transfer.observe;

import org.eclipse.dataspaceconnector.spi.observe.Observable;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

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
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#INITIAL INITIAL}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preCreated(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#PROVISIONING PROVISIONING}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preProvisioning(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#PROVISIONED PROVISIONED}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preProvisioned(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#REQUESTING REQUESTING}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preRequesting(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#REQUESTED REQUESTED}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preRequested(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#IN_PROGRESS IN_PROGRESS}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preInProgress(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#COMPLETED COMPLETED}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preCompleted(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#DEPROVISIONING DEPROVISIONING}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preDeprovisioning(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#DEPROVISIONED DEPROVISIONED}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preDeprovisioned(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#ENDED ENDED}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preEnded(TransferProcess process) {
    }

    /**
     * Called after a {@link TransferProcess} has moved to state
     * {@link TransferProcessStates#ERROR ERROR}, but before the change is persisted.
     *
     * @param process the transfer process whose state has changed.
     */
    default void preError(TransferProcess process) {
    }

}
