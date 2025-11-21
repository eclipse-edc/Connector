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

package org.eclipse.edc.iam.decentralizedclaims.lib;

import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * Default implementation of the {@link PresentationRequestService}. When receiving the
 * counter-party's SI token, generates the own SI token and requests the Verifiable Presentation
 * using this token.
 */
public class DefaultPresentationRequestService implements PresentationRequestService {

    private final SecureTokenService secureTokenService;
    private final CredentialServiceClient credentialServiceClient;
    private final CredentialServiceUrlResolver credentialServiceUrlResolver;

    public DefaultPresentationRequestService(SecureTokenService secureTokenService,
                                             CredentialServiceUrlResolver credentialServiceUrlResolver,
                                             CredentialServiceClient credentialServiceClient) {
        this.secureTokenService = secureTokenService;
        this.credentialServiceUrlResolver = credentialServiceUrlResolver;
        this.credentialServiceClient = credentialServiceClient;
    }

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                             String counterPartyDid, String counterPartyToken,
                                                                             List<String> scopes) {
        Map<String, Object> siTokenClaims = Map.of(PRESENTATION_TOKEN_CLAIM, counterPartyToken,
                ISSUED_AT, Instant.now().getEpochSecond(),
                AUDIENCE, counterPartyDid,
                ISSUER, ownDid,
                SUBJECT, ownDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).getEpochSecond());
        var siToken = secureTokenService.createToken(participantContextId, siTokenClaims, null);
        if (siToken.failed()) {
            return siToken.mapFailure();
        }
        var siTokenString = siToken.getContent().getToken();

        return credentialServiceUrlResolver.resolve(counterPartyDid)
                .compose(url -> credentialServiceClient.requestPresentation(url, siTokenString, scopes));
    }
}
