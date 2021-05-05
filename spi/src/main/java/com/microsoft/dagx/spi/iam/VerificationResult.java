/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.iam;

/**
 * The result of a token verification.
 */
public class VerificationResult {
    private boolean valid;
    private ClaimToken token;
    private String error;

    public VerificationResult(ClaimToken token) {
        valid = true;
        this.token = token;
    }

    public VerificationResult(String error) {
        valid = false;
        this.error = error;
    }

    public ClaimToken token() {
        return token;
    }

    public boolean valid() {
        return valid;
    }

    public String error() {
        return error;
    }
}


