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
 *       Cofinity-X - extract presentation request service
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.service;

import org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
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
 * This service is intended to be used together with the Decentralized Claims Protocols.
 * Details about the scope string can be found <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">here</a>
 */
public class DcpIdentityService implements IdentityService {
    private static final String SCOPE_STRING_REGEX = "(.+):(.+):(read|write|\\*)";

    private final SecureTokenService secureTokenService;
    private final Function<String, String> didResolver;
    private final TokenValidationAction tokenValidationAction;
    private final PresentationRequestService presentationRequestService;
    private final ClaimTokenCreatorFunction claimTokenCreatorFunction;
    private final VerifiableCredentialValidationService verifiableCredentialValidationService;

    /**
     * Constructs a new instance of the {@link DcpIdentityService}.
     *
     * @param secureTokenService Instance of an STS, which can create SI tokens
     * @param didResolver        Function that resolves the DID for a given participant context id
     */
    public DcpIdentityService(SecureTokenService secureTokenService, Function<String, String> didResolver,
                              TokenValidationAction tokenValidationAction,
                              PresentationRequestService presentationRequestService,
                              ClaimTokenCreatorFunction claimTokenCreatorFunction,
                              VerifiableCredentialValidationService verifiableCredentialValidationService) {
        this.secureTokenService = secureTokenService;
        this.didResolver = didResolver;
        this.tokenValidationAction = tokenValidationAction;
        this.presentationRequestService = presentationRequestService;
        this.claimTokenCreatorFunction = claimTokenCreatorFunction;
        this.verifiableCredentialValidationService = verifiableCredentialValidationService;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String participantContextId, TokenParameters parameters) {
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
        var claims = new HashMap<String, Object>();
        parameters.getClaims().forEach((k, v) -> claims.replace(k, v.toString()));

        var myOwnDid = didResolver.apply(participantContextId);

        claims.putAll(Map.of(
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                AUDIENCE, parameters.getStringClaim(AUDIENCE)));

        return secureTokenService.createToken(participantContextId, claims, scope)
                .map(originalToken -> originalToken.toBuilder()
                        .token("Bearer " + originalToken.getToken())
                        .build());
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(String participantContextId, TokenRepresentation tokenRepresentation, VerificationContext context) {
        // strip out the "Bearer " prefix
        var token = tokenRepresentation.getToken();
        if (!token.startsWith("Bearer ")) {
            return failure("Token is not a Bearer token");
        }
        token = token.replace("Bearer ", "").trim();
        tokenRepresentation = tokenRepresentation.toBuilder().token(token).build();
        var claimTokenResult = tokenValidationAction.validate(participantContextId, tokenRepresentation);

        if (claimTokenResult.failed()) {
            return claimTokenResult.mapEmpty();
        }

        var claimToken = claimTokenResult.getContent();
        var accessToken = claimToken.getStringClaim(PRESENTATION_TOKEN_CLAIM);
        var issuer = claimToken.getStringClaim(ISSUER);

        var myOwnDid = didResolver.apply(participantContextId);
        var requestedScopes = context.getScopes().stream().toList();

        var vpResponse = presentationRequestService.requestPresentation(participantContextId, myOwnDid, issuer, accessToken, requestedScopes);
        if (vpResponse.failed()) {
            return vpResponse.mapEmpty();
        }

        var presentations = vpResponse.getContent();

        // check all requested credentials are present
        var result = validateRequestedCredentials(presentations, requestedScopes)
                .compose(unused -> verifiableCredentialValidationService.validate(presentations, myOwnDid, getAdditionalValidations()));


        return result
                .compose(u -> verifyPresentationIssuer(issuer, presentations))
                .compose(u -> claimTokenCreatorFunction.apply(presentations.stream().map(p -> p.presentation().getCredentials().stream())
                        .reduce(Stream.empty(), Stream::concat)
                        .toList()));
    }

    private Result<Void> validateRequestedCredentials(List<VerifiablePresentationContainer> presentations, List<String> requestedScopes) {
        var allCreds = presentations.stream()
                .flatMap(p -> p.presentation().getCredentials().stream())
                .toList();
        if (requestedScopes.size() > allCreds.size()) {
            return Result.failure("Number of requested credentials does not match the number of returned credentials");
        }

        var types = allCreds.stream().map(VerifiableCredential::getType)
                .flatMap(Collection::stream)
                .distinct()
                .toList();


        return requestedScopes.stream().allMatch(scope -> types.stream().anyMatch(scope::contains)) ?
                Result.success() :
                Result.failure("Not all requested credentials are present in the presentation response");
    }

    /**
     * Checks that the issuer in the SI token == VP token issuer for all presentations
     */
    private Result<Void> verifyPresentationIssuer(String expectedIssuer, List<VerifiablePresentationContainer> presentationContainers) {

        var issuers = presentationContainers.stream().map(VerifiablePresentationContainer::presentation)
                .map(VerifiablePresentation::getHolder)
                .toList();

        if (issuers.stream().allMatch(expectedIssuer::equals)) {
            return Result.success();
        } else {
            return Result.failure("Returned presentations contains invalid issuer. Expected %s found %s".formatted(expectedIssuer, issuers));
        }
    }


    private Collection<? extends CredentialValidationRule> getAdditionalValidations() {
        return Collections.emptyList();
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
