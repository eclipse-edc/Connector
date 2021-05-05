/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema;

import com.microsoft.dagx.spi.DagxException;

public class SchemaValidationException extends DagxException {
    public SchemaValidationException(String message) {
        super(message);
    }
}
