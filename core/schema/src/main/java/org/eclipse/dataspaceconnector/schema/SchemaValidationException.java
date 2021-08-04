/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.schema;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class SchemaValidationException extends EdcException {
    public SchemaValidationException(String message) {
        super(message);
    }
}
