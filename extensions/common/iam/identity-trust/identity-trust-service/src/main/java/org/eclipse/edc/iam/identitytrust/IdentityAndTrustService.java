/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust;

import org.eclipse.edc.iam.identitytrust.validation.rules.HasValidIssuer;
import org.eclipse.edc.iam.identitytrust.validation.rules.HasValidSubjectIds;
import org.eclipse.edc.iam.identitytrust.validation.rules.IsNotExpired;
import org.eclipse.edc.iam.identitytrust.validation.rules.IsRevoked;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.validation.CredentialValidationRule;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.identitytrust.verification.PresentationVerifier;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implements an {@link IdentityService}, that:
 * <ul>
 *     <li>Obtains an SI token from a SecureTokenService</li>
 *     <li>Establishes proof-of-original possession, by extracting the access_token, and re-packaging it into a new SI token</li>
 *     <li>Performs a presentation request against a CredentialService</li>
 *     <li>Validates and verifies the VerifiablePresentation</li>
 * </ul>
 * This service is intended to be used together with the Identity And Trust Protocols.
 * Details about the scope string can be found <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">here</a>
 */
public class IdentityAndTrustService implements IdentityService {
    private static final String SCOPE_STRING_REGEX = "(.+):(.+):(read|write|\\*)";

    private final SecureTokenService secureTokenService;
    private final String myOwnDid;
    private final String participantId;
    private final PresentationVerifier presentationVerifier;
    private final CredentialServiceClient credentialServiceClient;
    private final JwtValidator jwtValidator;
    private final JwtVerifier jwtVerifier;
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    private final Clock clock;

    /**
     * Constructs a new instance of the {@link IdentityAndTrustService}.
     *
     * @param secureTokenService Instance of an STS, which can create SI tokens
     * @param myOwnDid           The DID which belongs to "this connector"
     */
    public IdentityAndTrustService(SecureTokenService secureTokenService, String myOwnDid, String participantId,
                                   PresentationVerifier presentationVerifier, CredentialServiceClient credentialServiceClient,
                                   JwtValidator jwtValidator, JwtVerifier jwtVerifier, TrustedIssuerRegistry trustedIssuerRegistry, Clock clock) {
        this.secureTokenService = secureTokenService;
        this.myOwnDid = myOwnDid;
        this.participantId = participantId;
        this.presentationVerifier = presentationVerifier;
        this.credentialServiceClient = credentialServiceClient;
        this.jwtValidator = jwtValidator;
        this.jwtVerifier = jwtVerifier;
        this.trustedIssuerRegistry = trustedIssuerRegistry;
        this.clock = clock;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var scope = parameters.getScope();
        var scopeValidationResult = validateScope(scope);

        if (scopeValidationResult.failed()) {
            return failure(scopeValidationResult.getFailureMessages());
        }

        // create claims for the STS
        var claims = new HashMap<String, String>();
        parameters.getAdditional().forEach((k, v) -> claims.replace(k, v.toString()));

        claims.putAll(Map.of(
                "iss", myOwnDid,
                "sub", myOwnDid,
                "aud", parameters.getAudience(),
                "client_id", participantId));

        return secureTokenService.createToken(claims, scope);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {

        // verify and validate incoming SI Token
        var issuerResult = jwtVerifier.verify(tokenRepresentation.getToken(), audience)
                .compose(v -> jwtValidator.validateToken(tokenRepresentation, audience))
                .compose(claimToken -> success(claimToken.getStringClaim("iss")));

        if (issuerResult.failed()) {
            return issuerResult.mapTo();
        }

        // todo: create SI Token, extract scope strings
        var vpResponse = credentialServiceClient.requestPresentation(null, null);

        if (vpResponse.failed()) {
            return vpResponse.mapTo();
        }

        var verifiablePresentation = vpResponse.getContent().get(0);
        var credentials = verifiablePresentation.presentation().getCredentials();
        // verify, that the VP and all VPs are cryptographically OK
        var result = presentationVerifier.verifyPresentation(verifiablePresentation)
                .compose(u -> {
                    // in addition, verify that all VCs are valid
                    var filters = new ArrayList<>(List.of(
                            new IsNotExpired(clock),
                            new HasValidSubjectIds(issuerResult.getContent()),
                            new IsRevoked(null),
                            new HasValidIssuer(getTrustedIssuerIds())));

                    filters.addAll(getAdditionalValidations());
                    var results = credentials.stream().map(c -> filters.stream().reduce(t -> Result.success(), CredentialValidationRule::and).apply(c)).reduce(Result::merge);

                    return results.orElseGet(() -> failure("Could not determine the status of the VC validation"));
                });

        //todo: at this point we have established what the other participant's DID is, and that it's authentic
        // so we need to make sure that `iss == sub == DID`
        return result.map(u -> extractClaimToken(credentials));
    }

    private ClaimToken extractClaimToken(List<VerifiableCredential> credentials) {
        return null;
    }

    private Collection<? extends CredentialValidationRule> getAdditionalValidations() {
        return List.of();
    }

    private List<String> getTrustedIssuerIds() {
        return trustedIssuerRegistry.getTrustedIssuers().stream().map(Issuer::id).toList();
    }

    private Result<Void> validateScope(String scope) {
        if (StringUtils.isNullOrBlank(scope)) {
            return failure("Scope string invalid: input string was null or empty");
        }
        return scope.matches(SCOPE_STRING_REGEX) ?
                success() :
                failure("Scope string invalid: '%s' does not match regex %s".formatted(scope, SCOPE_STRING_REGEX));
    }
}
