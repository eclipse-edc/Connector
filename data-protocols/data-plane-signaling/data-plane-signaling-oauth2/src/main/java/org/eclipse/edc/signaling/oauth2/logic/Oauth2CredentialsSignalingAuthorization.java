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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.signaling.spi.authorization.Header;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
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

    public Oauth2CredentialsSignalingAuthorization(Oauth2Client oauth2Client) {
        this.oauth2Client = oauth2Client;
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
     * @param headerGetter a function that retrieves an HTTP header value by name
     * @return a successful {@link Result} containing the {@code sub} claim string, or a failed
     *         result if the header is absent/malformed or the JWT cannot be parsed
     */
    @Override
    public Result<String> isAuthorized(Function<String, String> headerGetter) {
        var authorization = headerGetter.apply("Authorization");
        if (authorization == null || authorization.isBlank() || authorization.length() <= BEARER.length()) {
            return Result.failure("No valid Authorization header present");
        }

        var token = authorization.substring(BEARER.length());

        try {
            var jwt = SignedJWT.parse(token);
            var sub = jwt.getJWTClaimsSet().getClaims().get("sub");
            if (sub instanceof String callerId) {
                return Result.success(callerId);
            }
            return Result.failure("JWT sub claim %s is not a string".formatted(sub));
        } catch (ParseException e) {
            return Result.failure("JWT cannot be parsed correctly");
        }
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
}
