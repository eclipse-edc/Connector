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

package org.eclipse.edc.connector.dataplane.spi.pipeline;

/**
 * Used to create arguments for JUnit parameterized tests.
 */
public class StreamFailureArgument {
    private int code;
    private StreamFailure.Reason reason;

    public int getCode() {
        return code;
    }

    public StreamFailure.Reason getReason() {
        return reason;
    }

    public StreamFailureArgument(int code, StreamFailure.Reason reason) {
        this.code = code;
        this.reason = reason;
    }

}
