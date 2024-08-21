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

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@EndToEndTest
public class StsApiEndToEndTest extends StsEndToEndTestBase {

    public static final int PORT = getFreePort();
    public static final String BASE_STS = "http://localhost:" + PORT + "/sts";
    private static final String GRANT_TYPE = "client_credentials";


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

    @Test
    void requestToken() throws ParseException {
        var audience = "audience";
        var clientSecret = "client_secret";
        var expiresIn = 300;

        var client = initClient(clientSecret);

        var params = Map.of(
                "client_id", client.getClientId(),
                "audience", audience,
                "client_secret", clientSecret);

        var token = tokenRequest(params)
                .statusCode(200)
                .contentType(JSON)
                .body("access_token", notNullValue())
                .body("expires_in", is(expiresIn))
                .extract()
                .body()
                .jsonPath().getString("access_token");

        assertThat(parseClaims(token))
                .containsEntry(ISSUER, client.getDid())
                .containsEntry(SUBJECT, client.getDid())
                .containsEntry(AUDIENCE, List.of(audience))
                .doesNotContainKey(CLIENT_ID)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
    }


    @Test
    void requestToken_withBearerScope() throws ParseException {
        var clientSecret = "client_secret";
        var audience = "audience";
        var bearerAccessScope = "org.test.Member:read org.test.GoldMember:read";
        var expiresIn = 300;

        var client = initClient(clientSecret);


        var params = Map.of(
                "client_id", client.getClientId(),
                "audience", audience,
                "bearer_access_scope", bearerAccessScope,
                "client_secret", clientSecret);

        var token = tokenRequest(params)
                .statusCode(200)
                .contentType(JSON)
                .body("access_token", notNullValue())
                .body("expires_in", is(expiresIn))
                .extract()
                .body()
                .jsonPath().getString("access_token");


        assertThat(parseClaims(token))
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
    }

    @Test
    void requestToken_withAttachedAccessScope() throws IOException, ParseException {
        var clientSecret = "client_secret";
        var audience = "audience";
        var token = "test_token";
        var expiresIn = 300;
        var client = initClient(clientSecret);


        var params = Map.of(
                "client_id", client.getClientId(),
                "audience", audience,
                "token", token,
                "client_secret", clientSecret);

        var accessToken = tokenRequest(params)
                .statusCode(200)
                .contentType(JSON)
                .body("access_token", notNullValue())
                .body("expires_in", is(expiresIn))
                .extract()
                .body()
                .jsonPath().getString("access_token");


        assertThat(parseClaims(accessToken))
                .containsEntry(ISSUER, client.getDid())
                .containsEntry(SUBJECT, client.getDid())
                .containsEntry(AUDIENCE, List.of(audience))
                .doesNotContainKey(CLIENT_ID)
                .containsEntry(PRESENTATION_TOKEN_CLAIM, token)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
    }

    @Test
    void requestToken_shouldReturnError_whenClientNotFound() {

        var clientId = "client_id";
        var clientSecret = "client_secret";
        var audience = "audience";

        var params = Map.of(
                "client_id", clientId,
                "audience", audience,
                "client_secret", clientSecret);

        tokenRequest(params)
                .statusCode(401)
                .contentType(JSON);
    }

    protected ValidatableResponse tokenRequest(Map<String, String> params) {

        var req = baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE);
        params.forEach(req::formParam);
        return req.post("/token").then();
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .baseUri(BASE_STS)
                .when();
    }

    @Override
    protected RuntimePerClassExtension getRuntime() {
        return sts;
    }

}
