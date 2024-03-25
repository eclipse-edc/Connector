/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Key;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

public class DataPlanePublicApiEndToEndTest extends AbstractDataPlaneTest {

    public static final String PUBLIC_KEY_ALIAS = "public-key";
    public static final String PRIVATE_KEY_ALIAS = "1";
    // this is a data address representing the private backend for an HTTP pull transfer
    public static final DataAddress BACKEND_API_DATAADDRESS = DataAddress.Builder.newInstance()
            .type("HttpData")
            .property(EDC_NAMESPACE + "baseUrl", "https://jsonplaceholder.typicode.com/todos")
            .build();

    @Test
    void httpPull_missingToken_expect401() {
        DATAPLANE.getDataPlanePublicEndpoint()
                .baseRequest()
                .contentType(ContentType.JSON)
                /*.header(HttpHeaders.AUTHORIZATION, token) missing */
                .body("""
                        {
                           "bar": "baz"
                        }
                        """)
                .post("/v2/foo")
                .then()
                .statusCode(401)
                .body(Matchers.containsString("Missing Authorization Header"));
    }

    @Test
    void httpPull_invalidToken_expect403() {
        var token = "some-invalid-token";
        DATAPLANE.getDataPlanePublicEndpoint()
                .baseRequest()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .body("""
                        {
                           "bar": "baz"
                        }
                        """)
                .post("/v2/foo")
                .then()
                .statusCode(403);
    }

    @ParameterizedTest(name = "Method = {0}")
    @ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD" })
    void request_expect200(String method) {
        var token = createEdr();
        var body = DATAPLANE.getDataPlanePublicEndpoint()
                .baseRequest()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .request(method, "/v2/bar/baz")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();
        assertThat(body).isNotNull();
    }

    private Key resolvePrivateKey() {
        var privateKeyPem = runtime.getService(Vault.class).resolveSecret(PRIVATE_KEY_ALIAS);
        return new PemParser(mock()).parse(privateKeyPem).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

    /**
     * Creates and stores an EDR in the data plane. The serialized EDR (as serialized JWT) is returned. Token and the {@link AccessTokenData}
     * stored in the data plane are correlated via the "jti" claim in the token.
     *
     * @return The EDR in the form of a serialized JWT.
     */
    private String createEdr() {
        var tokenId = UUID.randomUUID().toString();
        // create JWT representing the EDR
        var jwt = createJwt(tokenId);

        // store the EDR
        var accessTokenStore = runtime.getService(AccessTokenDataStore.class);
        accessTokenStore.store(new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().build(), BACKEND_API_DATAADDRESS));
        return jwt;
    }

    private String createJwt(String tokenId) {

        try {
            var jwk = resolvePrivateKey();
            var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(PUBLIC_KEY_ALIAS).build();
            var claims = new JWTClaimsSet.Builder()
                    .issuer("me")
                    .subject("me")
                    .issueTime(new Date())
                    .jwtID(tokenId)
                    .build();

            var jwt = new SignedJWT(header, claims);
            jwt.sign(CryptoConverter.createSignerFor((PrivateKey) jwk));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}
