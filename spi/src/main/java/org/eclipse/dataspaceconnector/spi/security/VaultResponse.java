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

package org.eclipse.dataspaceconnector.spi.security;

/**
 * A response to a vault operation.
 */
public class VaultResponse {
    public static final VaultResponse OK = new VaultResponse();

    private final boolean success;
    private String error;

    public VaultResponse(String error) {
        success = false;
        this.error = error;
    }

    public VaultResponse() {
        success = true;
    }

    public boolean success() {
        return success;
    }

    public String error() {
        return error;
    }
}
