/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi;

/**
 * Base exception for the system.
 *
 * The system should use unchecked exceptions when appropriate (e.g., non-recoverable errors) and may extend this exception.
 */
public class EdcException extends RuntimeException {

    public EdcException(String message) {
        super(message);
    }

    public EdcException(String message, Throwable cause) {
        super(message, cause);
    }

    public EdcException(Throwable cause) {
        super(cause);
    }

    public EdcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
