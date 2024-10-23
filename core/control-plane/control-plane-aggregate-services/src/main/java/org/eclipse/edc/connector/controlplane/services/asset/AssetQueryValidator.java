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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.spi.result.Result;

import java.util.regex.Pattern;

import static java.lang.String.format;

public class AssetQueryValidator extends QueryValidator {
    private static final Pattern VALID_QUERY_PATH_REGEX = Pattern.compile("^[A-Za-z_']+.*$");

    public AssetQueryValidator() {
        super(Asset.class);
    }

    /**
     * Only paths are valid that start with either a character or an '_'
     *
     * @param path The path. Cannot start with anything other chan A-Za-z_
     */
    @Override
    protected Result<Void> isValid(String path) {
        if (VALID_QUERY_PATH_REGEX.matcher(path).matches()) {
            return Result.success();
        }
        return Result.failure(format("The query path must start with a letter or an '_' but was '%s'", path));
    }
}
