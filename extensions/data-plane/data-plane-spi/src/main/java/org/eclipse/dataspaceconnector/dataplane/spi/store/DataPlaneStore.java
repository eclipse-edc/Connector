/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.spi.store;

/**
 * Stores states of data flow requests.
 */
public interface DataPlaneStore {

    /**
     * Defines data flow states.
     */
    enum State {
        /**
         * There is no record of the process, i.e. one has not benn previously received.
         */
        NOT_TRACKED,

        /**
         * The process have been received.
         */
        RECEIVED,

        /**
         * The process has completed.
         */
        COMPLETED
    }

    /**
     * Mark the process as received.
     */
    void received(String processId);

    /**
     * Mark the process as completed.
     */
    void completed(String processId);

    /**
     * Returns the state of the process. If a process is unknown, returns {@link State#NOT_TRACKED}.
     */
    State getState(String processId);

}
