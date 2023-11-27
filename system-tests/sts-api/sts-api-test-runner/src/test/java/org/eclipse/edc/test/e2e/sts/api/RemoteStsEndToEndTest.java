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

package org.eclipse.edc.test.e2e.sts.api;

import org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.client.Oauth2ClientImpl;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.BEARER_ACCESS_ALIAS;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.PRESENTATION_ACCESS_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

@EndToEndTest
public class RemoteStsEndToEndTest extends StsEndToEndTestBase {

    public static final int PORT = getFreePort();
    public static final String STS_TOKEN_PATH = "http://localhost:" + PORT + "/sts/token";

    @RegisterExtension
    static EdcRuntimeExtension sts = new EdcRuntimeExtension(
            ":system-tests:sts-api:sts-api-test-runtime",
            "sts",
            new HashMap<>() {
                {
                    put("web.http.path", "/");
                    put("web.http.port", String.valueOf(getFreePort()));
                    put("web.http.sts.path", "/sts");
                    put("web.http.sts.port", String.valueOf(PORT));
                }
            }
    );
    private final StsRemoteClientConfiguration config = new StsRemoteClientConfiguration(STS_TOKEN_PATH, "client_id", "client_secret");

    private RemoteSecureTokenService remoteSecureTokenService;

    @BeforeEach
    void setup() {
        var oauth2Client = new Oauth2ClientImpl(testHttpClient(), new TypeManager());
        remoteSecureTokenService = new RemoteSecureTokenService(oauth2Client, config);
    }

    @Test
    void requestToken() {
        var audience = "audience";
        var params = Map.of(AUDIENCE, audience);

        var client = initClient(config.clientId(), config.clientSecret());

        assertThat(remoteSecureTokenService.createToken(params, null))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims)
                .satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getId())
                            .containsEntry(SUBJECT, client.getId())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .containsEntry(CLIENT_ID, client.getClientId())
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
                });

    }


    @Test
    void requestToken_withBearerScopeAndAlias() {
        var audience = "audience";
        var bearerAccessScope = "org.test.Member:read org.test.GoldMember:read";
        var bearerAccessAlias = "alias";
        var params = Map.of(AUDIENCE, audience, BEARER_ACCESS_ALIAS, bearerAccessAlias);

        var client = initClient(config.clientId(), config.clientSecret());

        assertThat(remoteSecureTokenService.createToken(params, bearerAccessScope))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims)
                .satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getId())
                            .containsEntry(SUBJECT, client.getId())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .containsEntry(CLIENT_ID, client.getClientId())
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                            .hasEntrySatisfying(PRESENTATION_ACCESS_TOKEN_CLAIM, (accessToken) -> {
                                assertThat(parseClaims((String) accessToken))
                                        .containsEntry(ISSUER, client.getId())
                                        .containsEntry(SUBJECT, bearerAccessAlias)
                                        .containsEntry(AUDIENCE, List.of(client.getClientId()))
                                        .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);

                            });
                });

    }

    @Test
    void requestToken_withAttachedAccessToken() {
        var audience = "audience";
        var accessToken = "test_token";
        var params = Map.of(
                AUDIENCE, audience,
                PRESENTATION_ACCESS_TOKEN_CLAIM, accessToken);

        var client = initClient(config.clientId(), config.clientSecret());

        assertThat(remoteSecureTokenService.createToken(params, null))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims).satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getId())
                            .containsEntry(SUBJECT, client.getId())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .containsEntry(CLIENT_ID, client.getClientId())
                            .containsEntry(PRESENTATION_ACCESS_TOKEN_CLAIM, accessToken)
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
                });
    }

    @Test
    void requestToken_shouldReturnError_whenClientNotFound() {
        var audience = "audience";
        var params = Map.of(AUDIENCE, audience);

        assertThat(remoteSecureTokenService.createToken(params, null)).isFailed()
                .extracting(Failure::getFailureDetail)
                .satisfies(failure -> assertThat(failure).contains("Invalid client"));

    }

    @Override
    protected EdcRuntimeExtension getRuntime() {
        return sts;
    }

}
