/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.ids.spi.daps;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
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
