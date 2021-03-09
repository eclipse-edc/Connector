package com.microsoft.dagx.spi.iam;

/**
 * The result of a token verification.
 */
public class VerificationResult {
    public static final VerificationResult VALID_TOKEN = new VerificationResult();

    private boolean valid;
    private String error;

    public VerificationResult(String error) {
        this.error = error;
    }

    VerificationResult() {
        valid = true;
    }

    public boolean valid() {
        return valid;
    }

    public String error() {
        return error;
    }
}


