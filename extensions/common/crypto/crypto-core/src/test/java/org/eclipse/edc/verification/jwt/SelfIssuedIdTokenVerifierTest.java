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

package org.eclipse.edc.verification.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.did.crypto.key.KeyConverter;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import static org.eclipse.edc.identitytrust.TestFunctions.createJwt;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelfIssuedIdTokenVerifierTest {

    private final PublicKeyResolver pkResolver = mock();
    private final SelfIssuedIdTokenVerifier verifier = new SelfIssuedIdTokenVerifier(pkResolver);
    private ECKey didVerificationMethod;

    @BeforeEach
    void setUp() throws JOSEException {
        didVerificationMethod = new ECKeyGenerator(Curve.P_256)
                .keyID("#my-key1")
                .generate();

        var vm = VerificationMethod.Builder.newInstance()
                .id("#my-key1")
                .type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019)
                .publicKeyJwk(didVerificationMethod.toPublicJWK().toJSONObject())
                .build();

        var publicKeyWrapper = KeyConverter.toPublicKeyWrapper(didVerificationMethod.toPublicJWK().toJSONObject(), "#my-key1");
        var publicKey = com.nimbusds.jose.jwk.KeyConverter.toJavaKeys(List.of(didVerificationMethod.toPublicJWK()))
                .stream().filter(k -> k instanceof PublicKey)
                .map(k -> (PublicKey) k)
                .findFirst()
                .orElseThrow();
        when(pkResolver.resolveKey(any())).thenReturn(Result.success(publicKey));
    }

    @Test
    void verify_succeeds() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                .issuer("test-iss")
                .audience("test-audience")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod).getToken(), "test-audience")).isSucceeded();
    }

    @Test
    void verify_verificationFailed_wrongSignature() throws JOSEException {
        var signKey = new ECKeyGenerator(Curve.P_256)
                .keyID("#my-key1")
                .generate();
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                .issuer("test-iss")
                .audience("test-audience")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        var jwt = createJwt(claimsSet, signKey);
        assertThat(verifier.verify(jwt.getToken(), "test-audience"))
                .isFailed()
                .detail().isEqualTo("Token could not be verified: Invalid signature");

    }

    @Test
    void verify_verificationFailed_missingAudClaim() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                .issuer("test-iss")
                // aud claim missing
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod).getToken(), "test-audience"))
                .isFailed()
                .detail().contains("Claim verification failed. JWT missing required claims: [aud]");
    }

    @Test
    void verify_verificationFailed_missingIssClaim() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                // iss claim missing
                .audience("test-audience")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod).getToken(), "test-audience"))
                .isFailed()
                .detail().contains("Claim verification failed. JWT missing required claims: [iss]");
    }

    @Test
    void verify_verificationFailed_wrongAudience() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                .issuer("test-iss")
                .audience("test-audience")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod).getToken(), "invalid-audience"))
                .isFailed()
                .detail().contains("Claim verification failed. JWT aud claim has value [test-audience], must be [invalid-audience]");
    }
}