/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.service;

import org.eclipse.dataspaceconnector.spi.query.QueryValidator;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;

import static java.lang.String.format;

class AssetQueryValidator extends QueryValidator {
    private static final List<String> KNOWN_PROPERTIES = List.of(
            Asset.PROPERTY_ID,
            Asset.PROPERTY_NAME,
            Asset.PROPERTY_DESCRIPTION,
            Asset.PROPERTY_VERSION,
            Asset.PROPERTY_CONTENT_TYPE
    );

    AssetQueryValidator() {
        super(Asset.class);
    }

    /**
     * The only valid paths are named properties from {@link Asset}
     *
     * @param path The path. Cannot start or end with a "."
     */
    @Override
    protected Result<Void> isValid(String path) {
        return KNOWN_PROPERTIES.contains(path) ? Result.success() :
                Result.failure(format("Currently only named properties of Asset are supported, and %s isn't one of them.", path));
    }
}
