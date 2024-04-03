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

package org.eclipse.edc.iam.identitytrust.service;

import org.eclipse.edc.iam.identitytrust.service.validation.rules.HasValidIssuer;
import org.eclipse.edc.iam.identitytrust.service.validation.rules.HasValidSubjectIds;
import org.eclipse.edc.iam.identitytrust.service.validation.rules.IsNotExpired;
import org.eclipse.edc.iam.identitytrust.service.validation.rules.IsNotRevoked;
import org.eclipse.edc.iam.identitytrust.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.spi.TrustedIssuerRegistry;
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.identitytrust.spi.verification.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListDatabase;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implements an {@link IdentityService}, that:
 * <ul>
 *     <li>Obtains an SI token from a SecureTokenService</li>
 *     <li>Establishes proof-of-original possession, by extracting the PRESENTATION_ACCESS_TOKEN_CLAIM, and re-packaging it into a new SI token</li>
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
    private final PresentationVerifier presentationVerifier;
    private final CredentialServiceClient credentialServiceClient;
    private final Function<TokenRepresentation, Result<ClaimToken>> tokenValidationAction;
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    private final Clock clock;
    private final CredentialServiceUrlResolver credentialServiceUrlResolver;
    private final ClaimTokenCreatorFunction claimTokenCreatorFunction;
    private final boolean strictRevocation = false;
    private final RevocationListDatabase revocationListDatabase;

    /**
     * Constructs a new instance of the {@link IdentityAndTrustService}.
     *
     * @param secureTokenService Instance of an STS, which can create SI tokens
     * @param myOwnDid           The DID which belongs to "this connector"
     */
    public IdentityAndTrustService(SecureTokenService secureTokenService, String myOwnDid,
                                   PresentationVerifier presentationVerifier, CredentialServiceClient credentialServiceClient,
                                   TokenValidationAction tokenValidationAction,
                                   TrustedIssuerRegistry trustedIssuerRegistry,
                                   Clock clock,
                                   CredentialServiceUrlResolver csUrlResolver,
                                   ClaimTokenCreatorFunction claimTokenCreatorFunction,
                                   RevocationListDatabase revocationListDatabase) {
        this.secureTokenService = secureTokenService;
        this.myOwnDid = myOwnDid;
        this.presentationVerifier = presentationVerifier;
        this.credentialServiceClient = credentialServiceClient;
        this.tokenValidationAction = tokenValidationAction;
        this.trustedIssuerRegistry = trustedIssuerRegistry;
        this.clock = clock;
        this.credentialServiceUrlResolver = csUrlResolver;
        this.claimTokenCreatorFunction = claimTokenCreatorFunction;
        this.revocationListDatabase = revocationListDatabase;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var aud = parameters.getStringClaim(AUDIENCE);
        var scope = parameters.getStringClaim(SCOPE);
        parameters = TokenParameters.Builder.newInstance()
                .claims(AUDIENCE, aud)
                .claims(SCOPE, scope)
                .claims(parameters.getClaims())
                .build();

        var scopeValidationResult = validateScope(scope);

        if (scopeValidationResult.failed()) {
            return failure(scopeValidationResult.getFailureMessages());
        }

        // create claims for the STS
        var claims = new HashMap<String, String>();
        parameters.getClaims().forEach((k, v) -> claims.replace(k, v.toString()));

        claims.putAll(Map.of(
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                AUDIENCE, parameters.getStringClaim(AUDIENCE)));

        return secureTokenService.createToken(claims, scope);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext context) {
        var claimTokenResult = tokenValidationAction.apply(tokenRepresentation);

        if (claimTokenResult.failed()) {
            return claimTokenResult.mapTo();
        }

        // create our own SI token, to request the VPs
        var claimToken = claimTokenResult.getContent();
        var accessToken = claimToken.getStringClaim(PRESENTATION_TOKEN_CLAIM);
        var issuer = claimToken.getStringClaim(ISSUER);

        var siTokenClaims = Map.of(PRESENTATION_TOKEN_CLAIM, accessToken,
                ISSUED_AT, Instant.now().toString(),
                AUDIENCE, issuer,
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        var siToken = secureTokenService.createToken(siTokenClaims, null);
        if (siToken.failed()) {
            return siToken.mapTo();
        }
        var siTokenString = siToken.getContent().getToken();

        // get CS Url, execute VP request
        var vpResponse = credentialServiceUrlResolver.resolve(issuer)
                .compose(url -> credentialServiceClient.requestPresentation(url, siTokenString, context.getScopes().stream().toList()));

        if (vpResponse.failed()) {
            return vpResponse.mapTo();
        }

        var presentations = vpResponse.getContent();
        var result = presentations.stream().map(verifiablePresentation -> {
            var credentials = verifiablePresentation.presentation().getCredentials();
            // verify, that the VP and all VPs are cryptographically OK
            return presentationVerifier.verifyPresentation(verifiablePresentation)
                    .compose(u -> validateVerifiableCredentials(credentials, issuer));
        }).reduce(Result.success(), Result::merge);
        //todo: at this point we have established what the other participant's DID is, and that it's authentic
        // so we need to make sure that `iss == sub == DID`
        return result.compose(u -> claimTokenCreatorFunction.apply(presentations.stream().map(p -> p.presentation().getCredentials().stream())
                .reduce(Stream.empty(), Stream::concat)
                .toList()));
    }

    @NotNull
    private Result<Void> validateVerifiableCredentials(List<VerifiableCredential> credentials, String issuer) {

        var revocationRule = new IsNotRevoked(revocationListDatabase);
        if (strictRevocation && credentials.stream().anyMatch(credential -> revocationRule.apply(credential).failed())) {
            return Result.failure("Encountered at least one revoked credential. Strict credential revocation check is activated.");
        }

        // in addition, verify that all VCs are valid
        var filters = new ArrayList<>(List.of(
                new IsNotExpired(clock),
                new HasValidSubjectIds(issuer),
                revocationRule, // either strict checking is disabled, or no violating credentials
                new HasValidIssuer(getTrustedIssuerIds())));


        filters.addAll(getAdditionalValidations());
        var results = credentials
                .stream()
                .map(c -> filters.stream().reduce(t -> Result.success(), CredentialValidationRule::and).apply(c))
                .reduce(Result::merge);
        return results.orElseGet(() -> failure("Could not determine the status of the VC validation"));
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
