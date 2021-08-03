/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.schema;

import org.eclipse.edc.spi.EdcException;

public class SchemaValidationException extends EdcException {
    public SchemaValidationException(String message) {
        super(message);
    }
}
