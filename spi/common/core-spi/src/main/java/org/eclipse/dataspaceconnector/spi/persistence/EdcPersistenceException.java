/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.persistence;

import org.eclipse.dataspaceconnector.spi.EdcException;

/**
 * Specialized exception for persistence subsystems.
 * <p>
 * Subsystems having responsibility of persisting state should throw
 * {@link EdcPersistenceException} wrapping checked {@link java.lang.Exception}s of an adapted system.
 */
public class EdcPersistenceException extends EdcException {
    public EdcPersistenceException(String message) {
        super(message);
    }

    public EdcPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EdcPersistenceException(Throwable cause) {
        super(cause);
    }

    public EdcPersistenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
