/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.identity;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;

/**
 * No-op service.
 */
public class NoopIdentityService implements IdentityService {
    private static final String TCK_PARTICIPANT_ID = "TCK_PARTICIPANT"; // the official TCK id

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String participantContextId, TokenParameters tokenParameters) {
        return Result.success(TokenRepresentation.Builder.newInstance().token("1234").expiresIn(Long.MAX_VALUE).build());
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(String participantContextId, TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        return Result.success(ClaimToken.Builder.newInstance().claim("client_id", TCK_PARTICIPANT_ID).build());
    }
}
