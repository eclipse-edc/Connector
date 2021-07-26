/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.security;

/**
 * A response to a vault operation.
 */
public class VaultResponse {
    public static final VaultResponse OK = new VaultResponse();

    private boolean success;
    private String error;

    public boolean success() {
        return success;
    }

    public String error() {
        return error;
    }

    public VaultResponse(String error) {
        success = false;
        this.error = error;
    }

    public VaultResponse() {
        success = true;
    }
}
