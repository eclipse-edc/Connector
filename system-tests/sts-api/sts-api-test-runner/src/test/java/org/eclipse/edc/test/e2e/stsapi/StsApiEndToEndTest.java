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

package org.eclipse.edc.test.e2e.stsapi;

import com.nimbusds.jwt.SignedJWT;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.identitytrust.sts.store.StsClientStore;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class StsApiEndToEndTest {

    public static final int PORT = getFreePort();
    public static final String BASE_STS = "http://localhost:" + PORT + "/sts";
    private static final String GRANT_TYPE = "client_credentials";

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

    @Test
    void requestToken() throws IOException, ParseException {
        var store = getClientStore();
        var vault = getVault();
        var clientId = "client_id";
        var clientSecret = "client_secret";
        var clientSecretAlias = "client_secret_alias";
        var audience = "audience";
        var expiresIn = 300;
        var client = createClient(clientId, clientSecretAlias);

        vault.storeSecret(clientSecretAlias, clientSecret);
        vault.storeSecret(client.getPrivateKeyAlias(), loadResourceFile("ec-privatekey.pem"));
        store.create(client);

        var token = baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", audience)
                .post("/token")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("access_token", notNullValue())
                .body("expires_in", is(expiresIn))
                .extract()
                .body()
                .jsonPath().getString("access_token");


        var jwt = SignedJWT.parse(token);

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, client.getId())
                .containsEntry(SUBJECT, client.getId())
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(CLIENT_ID, client.getClientId())
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
    }

    @Test
    void requestToken_shouldReturnError_whenClientNotFound() {

        var clientId = "client_id";
        var clientSecret = "client_secret";
        var audience = "audience";

        baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", audience)
                .post("/token")
                .then()
                .log().all(true)
                .statusCode(401)
                .contentType(JSON);
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .baseUri(BASE_STS)
                .when();
    }

    private StsClientStore getClientStore() {
        return sts.getContext().getService(StsClientStore.class);
    }

    private Vault getVault() {
        return sts.getContext().getService(Vault.class);
    }

    /**
     * Load content from a resource file.
     */
    private String loadResourceFile(String file) throws IOException {
        try (var resourceAsStream = StsApiEndToEndTest.class.getClassLoader().getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(resourceAsStream).readAllBytes());
        }
    }
}
