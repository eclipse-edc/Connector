/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.mock;

import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.spi.iam.ClaimToken;

/**
 * Extracts the participant id from a ClaimToken by retrieving a specific claim.
 */
public class MockParticipantIdExtractionFunction implements DefaultParticipantIdExtractionFunction {
    
    private final String identityClaimKey;
    
    public MockParticipantIdExtractionFunction(String identityClaimKey) {
        this.identityClaimKey = identityClaimKey;
    }
    
    @Override
    public String apply(ClaimToken claimToken) {
        return claimToken.getStringClaim(identityClaimKey);
    }
}
