/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.logic;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.keys.resolver.JwksPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.signaling.oauth2.DataPlaneSignalingOauth2Extension;
import org.eclipse.edc.signaling.spi.authorization.Header;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.Map;
import java.util.function.Function;

/**
 * {@link SignalingAuthorization} implementation for the OAuth2 Client Credentials grant type.
 *
 * <p>This implementation serves two roles in the Data Plane Signaling protocol:
 * <ul>
 *   <li><strong>Inbound authorization</strong> ({@link #isAuthorized}): validates that an incoming
 *       signaling request carries a Bearer JWT in its {@code Authorization} header and extracts the
 *       caller identity from the token's {@code sub} claim.</li>
 *   <li><strong>Outbound credential provisioning</strong> ({@link #evaluate}): obtains a fresh
 *       access token from an OAuth2 token endpoint using the client-credentials grant and returns
 *       it as a {@code Authorization: Bearer <token>} header to be attached to the outgoing
 *       signaling request.</li>
 * </ul>
 *
 * <p>The type identifier returned by {@link #getType()} is {@code "oauth2_client_credentials"},
 * which must match the {@link AuthorizationProfile#type()} value stored on the target
 * {@code DataPlaneInstance}.
 *
 * <p>The {@link AuthorizationProfile} passed to {@link #evaluate} must contain the following
 * properties:
 * <ul>
 *   <li>{@code tokenEndpoint} – URL of the OAuth2 token endpoint</li>
 *   <li>{@code clientId} – client identifier</li>
 *   <li>{@code clientSecret} – shared secret for the client</li>
 * </ul>
 */
public class Oauth2CredentialsSignalingAuthorization implements SignalingAuthorization {

    private static final String BEARER = "Bearer ";
    private final Oauth2Client oauth2Client;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final KeyParserRegistry keyParserRegistry;
    private final Monitor monitor;

    public Oauth2CredentialsSignalingAuthorization(Oauth2Client oauth2Client, TokenValidationService tokenValidationService,
                                                   TokenValidationRulesRegistry tokenValidationRulesRegistry,
                                                   KeyParserRegistry keyParserRegistry, Monitor monitor) {
        this.oauth2Client = oauth2Client;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.keyParserRegistry = keyParserRegistry;
        this.monitor = monitor;
    }

    /**
     * Returns {@code "oauth2_client_credentials"}, the type identifier for this strategy.
     */
    @Override
    public String getType() {
        return "oauth2_client_credentials";
    }

    /**
     * Validates the {@code Authorization} header of an incoming signaling request.
     *
     * <p>Expects a {@code Bearer <jwt>} value. The JWT is parsed and its {@code sub} claim is
     * returned as the caller identity on success.
     *
     * <p>If the {@code authorizationProfile} contains a {@code jwksUri} property, signature
     * verification is performed by fetching the JWKS from that URL. If it contains an inline
     * {@code jwks} property (a {@link Map} representing a JWK Set), the keys are used directly.
     * When neither is present the signature is not verified, as specified by the
     * Data Plane Signaling protocol.
     *
     * @param headerGetter         a function that retrieves an HTTP header value by name
     * @param authorizationProfile the authorization profile of the target Data Plane instance
     * @return a successful {@link Result} containing the {@code sub} claim string, or a failed
     *         result if the header is absent/malformed or the JWT cannot be validated
     */
    @Override
    public Result<String> isAuthorized(Function<String, String> headerGetter, AuthorizationProfile authorizationProfile) {
        var authorization = headerGetter.apply("Authorization");
        if (authorization == null || authorization.isBlank() || authorization.length() <= BEARER.length()) {
            return Result.failure("No valid Authorization header present");
        }

        var token = authorization.substring(BEARER.length());
        var rules = tokenValidationRulesRegistry.getRules(DataPlaneSignalingOauth2Extension.VALIDATION_RULES_CONTEXT);

        var resolverResult = buildPublicKeyResolver(authorizationProfile.properties());
        if (resolverResult.failed()) {
            return resolverResult.mapFailure();
        }

        var tokenValidation = tokenValidationService.validate(token, resolverResult.getContent(), rules);
        if (tokenValidation.failed()) {
            return tokenValidation.mapFailure();
        }
        var claimToken = tokenValidation.getContent();

        var sub = claimToken.getStringClaim("sub");
        if (sub == null) {
            return Result.failure("No 'sub' claim exists in the token");
        }
        return Result.success(sub);
    }

    /**
     * Obtains an OAuth2 access token for the given {@link AuthorizationProfile} and returns it as
     * an {@code Authorization: Bearer <token>} header.
     *
     * <p>Uses the OAuth2 Client Credentials grant via {@link Oauth2Client#requestToken}. The
     * {@code profile} must contain {@code tokenEndpoint}, {@code clientId}, and
     * {@code clientSecret} entries in its {@link AuthorizationProfile#properties()} map.
     *
     * @param profile the authorization profile of the target Data Plane instance
     * @return a successful {@link Result} containing the {@link Header} to attach to the outgoing
     *         request, or a failed result if the token request fails
     */
    @Override
    public Result<Header> evaluate(AuthorizationProfile profile) {
        var properties = profile.properties();
        var credentialsRequest = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .grantType("client_credentials")
                .url((String) properties.get("tokenEndpoint"))
                .clientId((String) properties.get("clientId"))
                .clientSecret((String) properties.get("clientSecret"))
                .build();

        return oauth2Client.requestToken(credentialsRequest)
                .map(token -> new Header("Authorization", "Bearer " + token.getToken()));
    }

    private Result<PublicKeyResolver> buildPublicKeyResolver(Map<String, Object> properties) {
        var jwksUri = (String) properties.get("jwksUri");
        if (jwksUri != null) {
            try {
                return Result.success(JwksPublicKeyResolver.create(keyParserRegistry, jwksUri, monitor));
            } catch (Exception e) {
                return Result.failure("Cannot create PublicKeyResolver for '" + jwksUri + "'. " + e.getMessage());
            }
        }

        var jwks = properties.get("jwks");
        if (jwks != null) {
            return parseJwks(jwks)
                    .map(ImmutableJWKSet::new)
                    .map(jwkSource -> JwksPublicKeyResolver.create(keyParserRegistry, monitor, jwkSource));
        }

        return Result.success(keyId -> Result.success(null));
    }

    private Result<JWKSet> parseJwks(Object jwks) {
        try {
            var jwkSet = JWKSet.parse((Map<String, Object>) jwks);
            return Result.success(jwkSet);
        } catch (Exception e) {
            return Result.failure("Failed to parse inline jwks: " + e.getMessage());
        }
    }

}
