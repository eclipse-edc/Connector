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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.identitytrust.validation.HasValidIssuer;
import org.eclipse.edc.iam.identitytrust.validation.HasValidSubjectIds;
import org.eclipse.edc.iam.identitytrust.validation.IsRevoked;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.validation.VcValidationRule;
import org.eclipse.edc.identitytrust.verifier.PresentationVerifier;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
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
    private final PresentationVerifier presentationVerifier;
    private final CredentialServiceClient credentialServiceClient;
    private final Monitor monitor;

    /**
     * Constructs a new instance of the {@link IdentityAndTrustService}.
     *
     * @param secureTokenService Instance of an STS, which can create SI tokens
     * @param myOwnDid           The DID which belongs to "this connector"
     */
    public IdentityAndTrustService(SecureTokenService secureTokenService, String myOwnDid, PresentationVerifier presentationVerifier, CredentialServiceClient credentialServiceClient, Monitor monitor) {
        this.secureTokenService = secureTokenService;
        this.myOwnDid = myOwnDid;
        this.presentationVerifier = presentationVerifier;
        this.credentialServiceClient = credentialServiceClient;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var scope = parameters.getScope();
        var scopeValidationResult = validateScope(scope);

        if (scopeValidationResult.failed()) {
            return failure(scopeValidationResult.getFailureMessages());
        }

        // create claims for the STS
        var claims = new java.util.HashMap<>(Map.of("iss", myOwnDid, "sub", myOwnDid, "aud", parameters.getAudience()));
        parameters.getAdditional().forEach((k, v) -> claims.replace(k, v.toString()));

        return secureTokenService.createToken(claims, scope);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
        SignedJWT jwt;
        try {
            // todo: implement validation of consumer's SI token
            jwt = SignedJWT.parse(tokenRepresentation.getToken());
        } catch (ParseException e) {
            monitor.severe("Error parsing JWT:", e);
            return Result.failure("Error parsing JWT");
        }
        var issuerResult = getIssuerDid(jwt);
        if (issuerResult.failed()) {
            return issuerResult.mapTo();
        }

        //todo: implement actual VP request, currently it's a stub
        // https://github.com/eclipse-edc/Connector/issues/3495
        var vpResponse = credentialServiceClient.requestPresentation(null, null, null);

        if (vpResponse.failed()) {
            return vpResponse.mapTo();
        }

        var verifiablePresentation = vpResponse.getContent();
        var credentials = verifiablePresentation.presentation().getCredentials();
        // verify, that the VP and all VPs are cryptographically OK
        var result = presentationVerifier.verifyPresentation(verifiablePresentation.rawVp(), verifiablePresentation.format())
                .compose(u -> {
                    // in addition, verify that all VCs are valid
                    var filters = new ArrayList<>(List.of(
                            new HasValidSubjectIds(issuerResult.getContent()),
                            new IsRevoked(null),
                            new HasValidIssuer(getAllowedIssuers())));

                    filters.addAll(getAdditionalValidations());
                    var results = credentials.stream().map(c -> filters.stream().reduce(t -> Result.success(), VcValidationRule::and).apply(c)).reduce(Result::merge);

                    return results.orElseGet(() -> failure("Could not determine the status of the VC validation"));
                });

        return result.map(u -> extractClaimToken(credentials));
    }

    private Result<String> getIssuerDid(SignedJWT tokenRepresentation) {

        try {
            return success(tokenRepresentation.getJWTClaimsSet().getIssuer());
        } catch (ParseException e) {
            monitor.severe("Error extracting issuer claim");
            return Result.failure("Failed to extract issuer claim");
        }
    }

    private ClaimToken extractClaimToken(List<VerifiableCredential> credentials) {
        return null;
    }

    private Collection<? extends VcValidationRule> getAdditionalValidations() {
        return List.of();
    }

    private List<String> getAllowedIssuers() {
        return List.of();
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
