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

package org.eclipse.dataspaceconnector.ids.core.daps;

import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.spi.Result;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.jetbrains.annotations.Nullable;

/**
 * A DAPS implementation that delegates to the underlying {@link IdentityService}.
 */
public class DapsServiceImpl implements DapsService {
    private final String connectorId;
    private final IdentityService identityService;

    public DapsServiceImpl(String connectorId, IdentityService identityService) {
        this.connectorId = connectorId;
        this.identityService = identityService;
    }

    @Override
    public Result<ClaimToken> verifyAndConvertToken(@Nullable String token) {
        if (token == null) {
            return Result.failure("No token provided");
        }

        return identityService.verifyJwtToken(token, connectorId);
    }
}
