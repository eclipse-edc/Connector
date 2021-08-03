/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.catalog.atlas.metadata;

import org.eclipse.edc.catalog.atlas.dto.AtlasErrorCode;
import org.eclipse.edc.spi.EdcException;

public class AtlasQueryException extends EdcException {
    private final AtlasErrorCode atlasErrorCode;

    public AtlasQueryException(AtlasErrorCode atlasErrorCode) {
        super("Error during an Apache Atlas API call: " + atlasErrorCode.getErrorMessage());
        this.atlasErrorCode = atlasErrorCode;
    }

    public AtlasErrorCode getAtlasErrorCode() {
        return atlasErrorCode;
    }
}
