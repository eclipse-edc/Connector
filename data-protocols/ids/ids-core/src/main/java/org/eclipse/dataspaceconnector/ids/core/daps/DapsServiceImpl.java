/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
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
    private String connectorName;
    private IdentityService identityService;

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
