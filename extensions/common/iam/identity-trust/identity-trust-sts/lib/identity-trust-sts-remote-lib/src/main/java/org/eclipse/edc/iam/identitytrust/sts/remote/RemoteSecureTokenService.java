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

import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.BEARER_ACCESS_SCOPE;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;

public class RemoteSecureTokenService implements SecureTokenService {

    public static final String GRANT_TYPE = "client_credentials";
    public static final String AUDIENCE_PARAM = "audience";

    private static final Map<String, String> CLAIM_MAPPING = Map.of(
            AUDIENCE, AUDIENCE_PARAM,
            PRESENTATION_TOKEN_CLAIM, PRESENTATION_TOKEN_CLAIM);

    private final Oauth2Client oauth2Client;
    private final StsRemoteClientConfiguration configuration;
    private final Vault vault;

    public RemoteSecureTokenService(Oauth2Client oauth2Client, StsRemoteClientConfiguration configuration, Vault vault) {
        this.oauth2Client = oauth2Client;
        this.configuration = configuration;
        this.vault = vault;
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, Object> claims, @Nullable String bearerAccessScope) {
        return createRequest(claims, bearerAccessScope)
                .compose(oauth2Client::requestToken);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createRequest(Map<String, Object> claims, @Nullable String bearerAccessScope) {

        var secret = vault.resolveSecret(configuration.clientSecretAlias());
        if (secret != null) {
            var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                    .url(configuration.tokenUrl())
                    .clientId(configuration.clientId())
                    .clientSecret(secret)
                    .grantType(GRANT_TYPE);

            var additionalParams = claims.entrySet().stream()
                    .filter(entry -> CLAIM_MAPPING.containsKey(entry.getKey()))
                    .map(entry -> Map.entry(CLAIM_MAPPING.get(entry.getKey()), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (bearerAccessScope != null) {
                additionalParams.put(BEARER_ACCESS_SCOPE, bearerAccessScope);
            }

            builder.params(additionalParams);
            return Result.success(builder.build());
        } else {
            return Result.failure("Failed to fetch client secret from the vault with alias: %s".formatted(configuration.clientSecretAlias()));
        }


    }
}
