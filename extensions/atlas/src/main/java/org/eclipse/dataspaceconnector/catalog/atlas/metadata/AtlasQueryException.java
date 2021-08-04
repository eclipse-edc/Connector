/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasErrorCode;
import org.eclipse.dataspaceconnector.spi.EdcException;

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
