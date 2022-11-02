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

package org.eclipse.edc.connector.dataplane.spi.store;

import java.util.Arrays;

/**
 * Stores states of data flow requests.
 */
public interface DataPlaneStore {

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

    /**
     * Defines data flow states.
     */
    enum State {
        /**
         * There is no record of the process, i.e. one has not benn previously received.
         */
        NOT_TRACKED(0),

        /**
         * The process have been received.
         */
        RECEIVED(100),

        /**
         * The process has completed.
         */
        COMPLETED(200);

        private final int code;

        State(int code) {
            this.code = code;
        }

        public static State from(int code) {
            return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
        }

        public int getCode() {
            return code;
        }
    }

}
