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
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.jetbrains.annotations.Nullable;

/**
 * A DAPS implementation that delegates to the underlying {@link IdentityService}.
 */
public class DapsServiceImpl implements DapsService {
    private final String connectorName;
    private final IdentityService identityService;

    public DapsServiceImpl(String connectorName, IdentityService identityService) {
        this.connectorName = connectorName;
        this.identityService = identityService;
    }

    @Override
    public VerificationResult verifyAndConvertToken(@Nullable String token) {
        if (token == null) {
            return new VerificationResult("No token provided");
        }

        return identityService.verifyJwtToken(token, connectorName);
    }
}
