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
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@EndToEndTest
public class RemoteStsEndToEndTest extends StsEndToEndTestBase {

    public static final int PORT = getFreePort();
    public static final String STS_TOKEN_PATH = "http://localhost:" + PORT + "/sts/token";
    private static final String SECRET = "secret";

    @RegisterExtension
    static RuntimePerClassExtension sts = new RuntimePerClassExtension(new EmbeddedRuntime(
            "sts",
            new HashMap<>() {
                {
                    put("web.http.path", "/");
                    put("web.http.port", String.valueOf(getFreePort()));
                    put("web.http.sts.path", "/sts");
                    put("web.http.sts.port", String.valueOf(PORT));
                }
            },
            ":system-tests:sts-api:sts-api-test-runtime"
    ));
    private final StsRemoteClientConfiguration config = new StsRemoteClientConfiguration(STS_TOKEN_PATH, "client_id", "client_secret_alias");
    private RemoteSecureTokenService remoteSecureTokenService;

    @BeforeEach
    void setup() {
        var oauth2Client = new Oauth2ClientImpl(testHttpClient(), new JacksonTypeManager());
        var vault = sts.getService(Vault.class);
        vault.storeSecret(config.clientSecretAlias(), SECRET);
        remoteSecureTokenService = new RemoteSecureTokenService(oauth2Client, config, vault);
    }

    @Test
    void requestToken() {
        var audience = "audience";
        var params = Map.of(AUDIENCE, audience);

        var client = initClient(config.clientId(), SECRET);

        assertThat(remoteSecureTokenService.createToken(params, null))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims)
                .satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getDid())
                            .containsEntry(SUBJECT, client.getDid())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .doesNotContainKey(CLIENT_ID)
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
                });

    }

    @Test
    void requestToken_withBearerScope() {
        var audience = "audience";
        var bearerAccessScope = "org.test.Member:read org.test.GoldMember:read";
        var params = Map.of(AUDIENCE, audience);

        var client = initClient(config.clientId(), SECRET);

        assertThat(remoteSecureTokenService.createToken(params, bearerAccessScope))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims)
                .satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getDid())
                            .containsEntry(SUBJECT, client.getDid())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .doesNotContainKey(CLIENT_ID)
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                            .hasEntrySatisfying(PRESENTATION_TOKEN_CLAIM, (accessToken) -> {
                                assertThat(parseClaims((String) accessToken))
                                        .containsEntry(ISSUER, client.getDid())
                                        .containsEntry(SUBJECT, audience)
                                        .containsEntry(AUDIENCE, List.of(client.getDid()))
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
                PRESENTATION_TOKEN_CLAIM, accessToken);

        var client = initClient(config.clientId(), SECRET);

        assertThat(remoteSecureTokenService.createToken(params, null))
                .isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .extracting(this::parseClaims).satisfies(claims -> {
                    assertThat(claims)
                            .containsEntry(ISSUER, client.getDid())
                            .containsEntry(SUBJECT, client.getDid())
                            .containsEntry(AUDIENCE, List.of(audience))
                            .doesNotContainKey(CLIENT_ID)
                            .containsEntry(PRESENTATION_TOKEN_CLAIM, accessToken)
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
    protected RuntimePerClassExtension getRuntime() {
        return sts;
    }

}
