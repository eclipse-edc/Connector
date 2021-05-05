/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.daps;

import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.iam.VerificationResult;
import org.jetbrains.annotations.Nullable;

/**
 * A Dynamic Attribute Provisioning Services as defined by IDS.
 */
public interface DapsService {

    /**
     * Verifies the token and returns a contained {@link ClaimToken} if valid.
     */
    VerificationResult verifyAndConvertToken(@Nullable String token);

}
