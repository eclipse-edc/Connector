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

package org.eclipse.edc.iam.identitytrust.sts.remote;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.BEARER_ACCESS_ALIAS;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.BEARER_ACCESS_SCOPE;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.PRESENTATION_ACCESS_TOKEN_CLAIM;

public class RemoteSecureTokenService implements SecureTokenService {

    public static final String GRANT_TYPE = "client_credentials";
    public static final String AUDIENCE_PARAM = "audience";

    private static final Map<String, String> CLAIM_MAPPING = Map.of(
            AUDIENCE, AUDIENCE_PARAM,
            BEARER_ACCESS_ALIAS, BEARER_ACCESS_ALIAS,
            PRESENTATION_ACCESS_TOKEN_CLAIM, PRESENTATION_ACCESS_TOKEN_CLAIM);

    private final Oauth2Client oauth2Client;
    private final StsRemoteClientConfiguration configuration;

    public RemoteSecureTokenService(Oauth2Client oauth2Client, StsRemoteClientConfiguration configuration) {
        this.oauth2Client = oauth2Client;
        this.configuration = configuration;
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, String> claims, @Nullable String bearerAccessScope) {
        return oauth2Client.requestToken(createRequest(claims, bearerAccessScope));
    }

    @NotNull
    private Oauth2CredentialsRequest createRequest(Map<String, String> claims, @Nullable String bearerAccessScope) {
        var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(configuration.tokenUrl())
                .clientId(configuration.clientId())
                .clientSecret(configuration.clientSecret())
                .grantType(GRANT_TYPE);

        var additionalParams = claims.entrySet().stream()
                .filter(entry -> CLAIM_MAPPING.containsKey(entry.getKey()))
                .map(entry -> Map.entry(CLAIM_MAPPING.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (bearerAccessScope != null) {
            additionalParams.put(BEARER_ACCESS_SCOPE, bearerAccessScope);
        }

        builder.params(additionalParams);
        return builder.build();
    }
}
