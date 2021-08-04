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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

import java.util.function.Consumer;

/**
 * Result of a connect operation.
 */
public class ConnectionResult {
    private final boolean success;
    private String error;
    private Consumer<byte[]> consumer;

    public ConnectionResult(Consumer<byte[]> consumer) {
        this.consumer = consumer;
        success = true;
    }

    public ConnectionResult(String error) {
        success = false;
        this.error = error;
    }

    /**
     * Returns true if the connection was successful.
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns an error if the connection was unsuccessful; otherwise null.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns a consumer that can be used to publish data to the topic.
     */
    public Consumer<byte[]> getConsumer() {
        return consumer;
    }


}
