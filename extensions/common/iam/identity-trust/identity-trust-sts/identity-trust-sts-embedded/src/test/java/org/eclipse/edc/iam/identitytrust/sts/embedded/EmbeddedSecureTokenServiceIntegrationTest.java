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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.JwtGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmbeddedSecureTokenServiceIntegrationTest {

    private final JtiValidationStore jtiValidationStore = mock();
    private KeyPair keyPair;
    private EmbeddedSecureTokenService secureTokenService;

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("RSA");
        return gen.generateKeyPair();
    }

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        keyPair = generateKeyPair();
        var tokenGenerationService = new JwtGenerationService(s -> Result.success(new RSASSASigner(keyPair.getPrivate())));
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());
        secureTokenService = new EmbeddedSecureTokenService(tokenGenerationService, () -> "test-private-keyid", () -> "test-keyid", Clock.systemUTC(), 10 * 60, jtiValidationStore);
    }

    @Test
    void createToken_withoutBearerAccessScope() {
        var issuer = "testIssuer";

        var claims = Map.of(ISSUER, issuer);
        var tokenResult = secureTokenService.createToken(claims, null);

        assertThat(tokenResult).isSucceeded()
                .satisfies(tokenRepresentation -> {
                    var jwt = SignedJWT.parse(tokenRepresentation.getToken());
                    assertThat(jwt.verify(createVerifier(jwt.getHeader(), keyPair.getPublic()))).isTrue();
                    assertThat(jwt.getJWTClaimsSet().getClaims())
                            .containsEntry(ISSUER, issuer)
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                            .doesNotContainKey(PRESENTATION_TOKEN_CLAIM);
                });

    }

    @Test
    void createToken_withBearerAccessScope() {
        var scopes = "email:read";
        var issuer = "testIssuer";
        var audience = "audience";
        var claims = Map.of(ISSUER, issuer, AUDIENCE, audience);
        var tokenResult = secureTokenService.createToken(claims, scopes);

        assertThat(tokenResult).isSucceeded()
                .satisfies(tokenRepresentation -> {
                    var jwt = SignedJWT.parse(tokenRepresentation.getToken());
                    assertThat(jwt.verify(createVerifier(jwt.getHeader(), keyPair.getPublic()))).isTrue();

                    assertThat(jwt.getJWTClaimsSet().getClaims())
                            .containsEntry(ISSUER, issuer)
                            .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                            .extractingByKey(PRESENTATION_TOKEN_CLAIM, as(STRING))
                            .satisfies(accessToken -> {
                                var accessTokenJwt = SignedJWT.parse(accessToken);
                                assertThat(accessTokenJwt.verify(createVerifier(accessTokenJwt.getHeader(), keyPair.getPublic()))).isTrue();
                                assertThat(accessTokenJwt.getJWTClaimsSet().getClaims())
                                        .containsEntry(ISSUER, issuer)
                                        .containsEntry(SUBJECT, audience)
                                        .containsEntry(AUDIENCE, List.of(issuer))
                                        .containsEntry(SCOPE, scopes)
                                        .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);
                            });
                });
    }

    @ParameterizedTest
    @ArgumentsSource(ClaimsArguments.class)
    void createToken_shouldFail_withMissingClaims(Map<String, String> claims) {
        var tokenResult = secureTokenService.createToken(claims, "email:read");
        assertThat(tokenResult).isFailed()
                .satisfies(f -> assertThat(f.getFailureDetail()).matches("Missing [a-z]* in the input claims"));
    }

    private JWSVerifier createVerifier(JWSHeader header, Key publicKey) throws JOSEException {
        return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
    }

    private static class ClaimsArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(Map.of(ISSUER, "iss"), Map.of(AUDIENCE, "aud")).map(Arguments::of);
        }
    }
}
