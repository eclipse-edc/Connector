/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.signaling.oauth2.tokenexchange.logic;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.TokenExchangeOauth2CredentialsRequest;
import org.eclipse.edc.keys.resolver.JwksPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.signaling.oauth2.tokenexchange.DataPlaneSignalingOauth2TokenExchangeExtension;
import org.eclipse.edc.signaling.spi.authorization.Header;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.IssuerEqualsValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * {@link SignalingAuthorization} implementation for the OAuth2 Token Exchange (RFC 8693) profile of the Data Plane
 * Signaling specification.
 *
 * <p>In this profile the issuing party does not run a self-signing authorization server. Instead it presents a
 * workload-layer credential (a projected Kubernetes ServiceAccount token, a SPIFFE JWT-SVID, ...) to a
 * <em>token exchange broker</em>, which applies policy and mints a short-lived scoped JWT. The workload credential
 * never travels to the receiving party.
 *
 * <p>The type identifier returned by {@link #getType()} is {@code "oauth2_token_exchange"}, which must match the
 * {@link AuthorizationProfile#type()} value stored on the target {@code DataPlaneInstance}.
 *
 * <p>The {@link AuthorizationProfile} carries the broker coordinates and the exchange parameters:
 * <ul>
 *   <li>{@code tokenExchangeEndpoint} - URL of the broker's token exchange endpoint (required)</li>
 *   <li>{@code issuer} - the expected {@code iss} claim of tokens minted by the broker (required)</li>
 *   <li>{@code jwksUri} - URL of the broker's JWKS endpoint (required, inline {@code jwks} is not permitted)</li>
 *   <li>{@code resource} - the {@code id} URI of the Principal Resource the exchanged token speaks for (required
 *       for outbound requests)</li>
 *   <li>{@code audience} - the audience the exchanged token is requested for, optional, falls back to the
 *       runtime-wide default audience</li>
 *   <li>{@code scope} - optional, falls back to the runtime-wide default scope. Outbound requests fail when neither
 *       is present</li>
 * </ul>
 *
 * <p>The workload credential itself is deployment-local and is never part of the registered profile, it is supplied
 * by a {@link WorkloadCredentialSource}.
 *
 * <p>The caller identity reported by {@link #isAuthorized} is the RFC 8693 {@code client_id} claim, falling back to
 * {@code sub} when the broker does not mint one. This lets {@code sub} stay a Principal Resource URI as the
 * specification requires, while {@code client_id} names the data plane / control plane the token speaks for, which is
 * what the caller identity is matched against. Brokers that only mint {@code sub} still work, provided the Principal
 * Resource is named with that identifier.
 */
public class Oauth2TokenExchangeSignalingAuthorization implements SignalingAuthorization {

    public static final String TYPE = "oauth2_token_exchange";

    static final String TOKEN_EXCHANGE_ENDPOINT = "tokenExchangeEndpoint";
    static final String ISSUER = "issuer";
    static final String JWKS_URI = "jwksUri";
    static final String JWKS = "jwks";
    static final String RESOURCE = "resource";
    static final String AUDIENCE = "audience";
    static final String SCOPE = "scope";

    private static final String BEARER = "Bearer ";

    private final Oauth2Client oauth2Client;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final KeyParserRegistry keyParserRegistry;
    private final WorkloadCredentialSource workloadCredentialSource;
    private final Monitor monitor;
    private final @Nullable String defaultScope;
    private final String defaultAudience;

    public Oauth2TokenExchangeSignalingAuthorization(Oauth2Client oauth2Client,
                                                     TokenValidationService tokenValidationService,
                                                     TokenValidationRulesRegistry tokenValidationRulesRegistry,
                                                     KeyParserRegistry keyParserRegistry,
                                                     WorkloadCredentialSource workloadCredentialSource,
                                                     Monitor monitor,
                                                     String defaultAudience, @Nullable String defaultScope) {
        this.oauth2Client = oauth2Client;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.keyParserRegistry = keyParserRegistry;
        this.workloadCredentialSource = workloadCredentialSource;
        this.monitor = monitor;
        this.defaultAudience = defaultAudience;
        this.defaultScope = defaultScope;
    }

    /**
     * Returns {@code "oauth2_token_exchange"}, the type identifier for this strategy.
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Validates the {@code Authorization} header of an incoming signaling request as described by the
     * <em>Receiver Validation</em> section of the profile: the JWT signature is verified against the keys published
     * at {@code jwksUri} and the {@code iss} claim must match the registered {@code issuer}. The {@code aud} claim is
     * not verified: it names the token exchange service's audience, not the receiving party.
     *
     * <p>The caller identity is taken from the {@code client_id} claim, or from {@code sub} when {@code client_id} is
     * absent.
     *
     * @param headerGetter         a function that retrieves an HTTP header value by name
     * @param authorizationProfile the authorization profile of the target Data Plane instance
     * @return a successful {@link Result} containing the caller identity, or a failed result
     */
    @Override
    public Result<String> isAuthorized(Function<String, String> headerGetter, AuthorizationProfile authorizationProfile) {
        var authorization = headerGetter.apply("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return Result.failure("No valid Authorization header present");
        }

        var properties = authorizationProfile.properties();
        if (properties.get(JWKS) != null) {
            return Result.failure("Inline '%s' is not permitted by the '%s' authorization profile, use '%s' instead"
                    .formatted(JWKS, TYPE, JWKS_URI));
        }

        var jwksUri = mandatory(properties, JWKS_URI);
        if (jwksUri.failed()) {
            return jwksUri.mapFailure();
        }

        var issuer = mandatory(properties, ISSUER);
        if (issuer.failed()) {
            return issuer.mapFailure();
        }

        var resolver = buildPublicKeyResolver(jwksUri.getContent());
        if (resolver.failed()) {
            return resolver.mapFailure();
        }

        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(DataPlaneSignalingOauth2TokenExchangeExtension.VALIDATION_RULES_CONTEXT));
        rules.add(new IssuerEqualsValidationRule(issuer.getContent()));

        var token = authorization.substring(BEARER.length());
        return tokenValidationService.validate(token, resolver.getContent(), rules)
                .compose(claimToken -> {
                    var callerId = claimToken.getStringClaim(CLIENT_ID);
                    if (callerId == null) {
                        callerId = claimToken.getStringClaim(SUBJECT);
                    }
                    if (callerId == null) {
                        return Result.failure("Neither a '%s' nor a '%s' claim exists in the token".formatted(CLIENT_ID, SUBJECT));
                    }
                    return Result.success(callerId);
                });
    }

    /**
     * Exchanges the workload credential for a scoped JWT at the broker and returns it as an
     * {@code Authorization: Bearer <token>} header.
     *
     * <p>Exchanged tokens are not cached: every signaling request triggers a fresh exchange, and the workload
     * credential is re-read from its source before each one.
     *
     * @param profile the authorization profile of the target Data Plane instance
     * @return a successful {@link Result} containing the {@link Header} to attach, or a failed result
     */
    @Override
    public Result<Header> evaluate(AuthorizationProfile profile) {
        var properties = profile.properties();

        var endpoint = mandatory(properties, TOKEN_EXCHANGE_ENDPOINT);
        if (endpoint.failed()) {
            return endpoint.mapFailure();
        }

        var resource = mandatory(properties, RESOURCE);
        if (resource.failed()) {
            return resource.mapFailure();
        }

        var audience = optional(properties, AUDIENCE, defaultAudience);

        var scope = optional(properties, SCOPE, defaultScope);
        if (scope == null) {
            return Result.failure("Mandatory property '%s' is missing on the '%s' authorization profile and no default scope is configured".formatted(SCOPE, TYPE));
        }
        return obtainToken(endpoint.getContent(), resource.getContent(), audience, scope)
                .map(token -> new Header("Authorization", BEARER + token));
    }

    private Result<String> obtainToken(String endpoint, String resource, String audience, String scope) {
        return workloadCredentialSource.get().compose(subjectToken -> {
            var request = TokenExchangeOauth2CredentialsRequest.Builder.newInstance()
                    .url(endpoint)
                    .scope(scope)
                    .resource(resource)
                    .audience(audience)
                    .subjectToken(subjectToken)
                    .build();

            return oauth2Client.requestToken(request).map(tokenRepresentation -> tokenRepresentation.getToken());
        });
    }

    private Result<PublicKeyResolver> buildPublicKeyResolver(String jwksUri) {
        try {
            return Result.success(JwksPublicKeyResolver.create(keyParserRegistry, jwksUri, monitor));
        } catch (Exception e) {
            return Result.failure("Cannot create PublicKeyResolver for '" + jwksUri + "'. " + e.getMessage());
        }
    }

    private Result<String> mandatory(Map<String, Object> properties, String key) {
        var value = properties.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return Result.success(string);
        }
        return Result.failure("Mandatory property '%s' is missing on the '%s' authorization profile".formatted(key, TYPE));
    }

    private @Nullable String optional(Map<String, Object> properties, String key, @Nullable String fallback) {
        var value = properties.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        return fallback;
    }
}
