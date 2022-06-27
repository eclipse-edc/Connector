/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.iam.did.crypto.credentials;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyPairFactory.generateKeyPairP256;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifiableCredentialFactoryTest {
    private static final Faker FAKER = new Faker();

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private EcPrivateKeyWrapper privateKey;
    private EcPublicKeyWrapper publicKey;

    @BeforeEach
    void setup() throws JOSEException {
        this.privateKey = new EcPrivateKeyWrapper((ECKey) getJwk("private_p256.pem"));
        this.publicKey = new EcPublicKeyWrapper((ECKey) getJwk("public_p256.pem"));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void createVerifiableCredential() throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);

        assertThat(vc).isNotNull();
        assertThat(vc.getJWTClaimsSet().getIssuer()).isEqualTo("test-connector");
        assertThat(vc.getJWTClaimsSet().getSubject()).isEqualTo("verifiable-credential");
        assertThat(vc.getJWTClaimsSet().getAudience()).containsExactly("test-audience");
        assertThat(vc.getJWTClaimsSet().getJWTID()).satisfies(UUID::fromString);
        assertThat(vc.getJWTClaimsSet().getExpirationTime()).isEqualTo(now.plus(10, MINUTES).truncatedTo(SECONDS));
    }

    @Test
    void ensureSerialization() throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);

        assertThat(vc).isNotNull();
        String jwtString = vc.serialize();

        //deserialize
        var deserialized = SignedJWT.parse(jwtString);

        assertThat(deserialized.getJWTClaimsSet()).isEqualTo(vc.getJWTClaimsSet());
        assertThat(deserialized.getHeader().getAlgorithm()).isEqualTo(vc.getHeader().getAlgorithm());
        assertThat(deserialized.getPayload().toString()).isEqualTo(vc.getPayload().toString());
    }

    @Test
    void verifyJwt_OnInvalidSignature_fails() {
        var jwt = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);
        var unrelatedPublicKey = new EcPublicKeyWrapper(generateKeyPairP256());
        assertThat(VerifiableCredentialFactory.verify(jwt, unrelatedPublicKey, "test-audience").getFailureMessages()).containsExactly("Invalid signature");
    }

    @Test
    void verifyJwt_OnVerificationFailure_fails() throws Exception {
        var jwt = mock(SignedJWT.class);
        var message = FAKER.lorem().sentence();
        when(jwt.verify(any())).thenThrow(new JOSEException(message));
        assertThat(VerifiableCredentialFactory.verify(jwt, publicKey, "test-audience").getFailureMessages())
                .containsExactly("Unable to verify JWT token. " + message);

    }

    @Test
    void verifyJwt_OnInvalidClaims_fails() throws Exception {
        var jwt = mock(SignedJWT.class);
        var message = FAKER.lorem().sentence();
        when(jwt.verify(any())).thenReturn(true);
        when(jwt.getJWTClaimsSet()).thenThrow(new ParseException(message, 0));
        assertThat(VerifiableCredentialFactory.verify(jwt, publicKey, "test-audience").getFailureMessages())
                .containsExactly("Error verifying JWT token. The payload must represent a valid JSON object and a JWT claims set. " + message);
    }

    @ParameterizedTest(name = "{2}")
    @ArgumentsSource(ClaimsArgs.class)
    void verifyJwt_OnClaims(UnaryOperator<JWTClaimsSet.Builder> builderOperator, boolean expectSuccess, String ignoredName) throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);

        var claimsSetBuilder = new JWTClaimsSet.Builder(vc.getJWTClaimsSet());
        var claimsSet = builderOperator.apply(claimsSetBuilder).build();

        var jwt = new SignedJWT(vc.getHeader(), claimsSet);
        jwt.sign(privateKey.signer());

        var result = VerifiableCredentialFactory.verify(jwt, publicKey, "test-audience");
        assertThat(result.succeeded()).isEqualTo(expectSuccess);
        if (!expectSuccess) {
            assertThat(result.getFailureMessages())
                    .isNotEmpty()
                    .allMatch(m -> m.startsWith("Claim verification failed. "));
        }
    }

    static class ClaimsArgs implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    jwtCase(b -> b, true, "valid token"),
                    jwtCase(b -> b.audience(List.of()), false, "empty audience"),
                    jwtCase(b -> b.audience(List.of("https://otherserver.com")), false, "wrong audience"),
                    jwtCase(b -> b.audience(List.of("test-audience")), true, "expected audience"), // sanity check
                    jwtCase(b -> b.subject(null), false, "empty subject"),
                    jwtCase(b -> b.subject("other-subject"), false, "wrong subject"),
                    jwtCase(b -> b.subject("verifiable-credential"), true, "expected subject"), // sanity check
                    jwtCase(b -> b.issuer(null), false, "empty issuer"),
                    jwtCase(b -> b.issuer("other-issuer"), true, "other issuer"),
                    jwtCase(b -> b.expirationTime(null), false, "empty expiration"),
                    // Nimbus library allows (by default) max 60 seconds of expiration date clock skew
                    jwtCase(b -> b.expirationTime(Date.from(Instant.now().minus(61, SECONDS))), false, "past expiration beyond max skew"),
                    jwtCase(b -> b.expirationTime(Date.from(Instant.now().minus(1, SECONDS))), true, "past expiration within max skew"),
                    jwtCase(b -> b.expirationTime(Date.from(Instant.now().plus(1, MINUTES))), true, "future expiration"),
                    jwtCase(b -> b.claim("foo", "bar"), true, "additional claim")
            );
        }
    }

    @NotNull
    private static Arguments jwtCase(UnaryOperator<JWTClaimsSet.Builder> builderOperator, boolean expectSuccess, String name) {
        return Arguments.of(builderOperator, expectSuccess, name);
    }

    private JWK getJwk(String resourceName) throws JOSEException {
        String privateKeyPem = getResourceFileContentAsString(resourceName);
        return JWK.parseFromPEMEncodedObjects(privateKeyPem);
    }
}
