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

package org.eclipse.edc.spi;

/**
 * Base exception for the system.
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
