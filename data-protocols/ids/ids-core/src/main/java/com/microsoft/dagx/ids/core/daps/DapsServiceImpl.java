/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.daps;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.VerificationResult;
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
