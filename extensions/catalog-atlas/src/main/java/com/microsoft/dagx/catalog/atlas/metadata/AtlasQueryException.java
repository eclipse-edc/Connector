/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.catalog.atlas.dto.AtlasErrorCode;
import com.microsoft.dagx.spi.DagxException;

public class AtlasQueryException extends DagxException {
    private final AtlasErrorCode atlasErrorCode;

    public AtlasQueryException(AtlasErrorCode atlasErrorCode) {
        super("Error during an Apache Atlas API call: " + atlasErrorCode.getErrorMessage());
        this.atlasErrorCode = atlasErrorCode;
    }

    public AtlasErrorCode getAtlasErrorCode() {
        return atlasErrorCode;
    }
}
