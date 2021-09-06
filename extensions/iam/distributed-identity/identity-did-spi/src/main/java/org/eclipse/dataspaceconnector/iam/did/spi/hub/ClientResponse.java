/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.spi.hub;

/**
 * A response emitted by a hub operation.
 */
public class ClientResponse<T> {
    private T response;
    private String error;

    public T getResponse() {
        return response;
    }

    public boolean isError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    public ClientResponse(T response) {
        this.response = response;
    }

    public ClientResponse(String error) {
        this.error = error;
    }
}
