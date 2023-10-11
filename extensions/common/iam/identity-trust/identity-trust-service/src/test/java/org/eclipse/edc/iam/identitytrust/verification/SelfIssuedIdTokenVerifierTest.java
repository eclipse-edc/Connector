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

package org.eclipse.edc.iam.identitytrust.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.eclipse.edc.identitytrust.TestFunctions.createJwt;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelfIssuedIdTokenVerifierTest {

    private final DidResolverRegistry didResolverRegistry = mock();
    private final SelfIssuedIdTokenVerifier verifier = new SelfIssuedIdTokenVerifier(didResolverRegistry);
    private ECKey didVerificationMethod;

    @BeforeEach
    void setUp() throws JOSEException {
        didVerificationMethod = new ECKeyGenerator(Curve.P_256)
                .keyID("#my-key1")
                .generate();

        var vm = VerificationMethod.Builder.create()
                .id("#my-key1")
                .type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019)
                .publicKeyJwk(didVerificationMethod.toPublicJWK().toJSONObject())
                .build();

        when(didResolverRegistry.resolve(any())).thenReturn(Result.success(DidDocument.Builder.newInstance()
                .verificationMethod(List.of(vm))
                .service(Collections.singletonList(new Service("#my-service1", "MyService", "http://doesnotexi.st")))
                .build()));
    }

    @Test
    void verify_succeeds() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("test-sub")
                .issuer("test-iss")
                .audience("test-audience")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod), "test-audience")).isSucceeded();
    }

    @Test
    void verify_didResolutionFailed() {
        when(didResolverRegistry.resolve(any())).thenReturn(Result.failure("test failure"));
        var jwt = createJwt();
        assertThat(verifier.verify(jwt, "test-audience")).isFailed()
                .detail()
                .isEqualTo("Unable to resolve DID: test failure");
    }

    @Test
    void verify_publicKeyNotFound() {
        assertThat(verifier.verify(createJwt(), "test-audience")).isFailed()
                .detail()
                .isEqualTo("Public Key not found in DID Document.");
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
        assertThat(verifier.verify(jwt, "test-audience"))
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
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod), "test-audience"))
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
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod), "test-audience"))
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
        assertThat(verifier.verify(createJwt(claimsSet, didVerificationMethod), "invalid-audience"))
                .isFailed()
                .detail().contains("Claim verification failed. JWT aud claim has value [test-audience], must be [invalid-audience]");
    }
}