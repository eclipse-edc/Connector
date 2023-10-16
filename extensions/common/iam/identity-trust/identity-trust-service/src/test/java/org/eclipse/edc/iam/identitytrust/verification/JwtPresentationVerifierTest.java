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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.JwtCreationUtils;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtPresentationVerifierTest {
    public static final String CENTRAL_ISSUER_KEY_ID = "#central-issuer-key1";
    public static final String PRESENTER_KEY_ID = "#my-key1";
    private static final String VP_HOLDER_ID = "did:web:test-issuer";
    private static final String MY_OWN_DID = "did:web:myself";
    private static final String CENTRAL_ISSUER_DID = "did:web:some-official-issuer";
    private final DidResolverRegistry didRegistryMock = mock();
    private ECKey vpSigningKey;
    private ECKey vcSigningKey;

    @BeforeEach
    void setup() throws JOSEException {
        vpSigningKey = new ECKeyGenerator(Curve.P_256)
                .keyID(PRESENTER_KEY_ID)
                .generate();

        vcSigningKey = new ECKeyGenerator(Curve.P_256)
                .keyID(CENTRAL_ISSUER_KEY_ID)
                .generate();


        // the DID document of the VP presenter (i.e. a participant agent)
        var vpPresenterDid = DidDocument.Builder.newInstance()
                .verificationMethod(List.of(VerificationMethod.Builder.create()
                        .id(PRESENTER_KEY_ID)
                        .type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019)
                        .publicKeyJwk(vpSigningKey.toPublicJWK().toJSONObject())
                        .build()))
                .service(Collections.singletonList(new Service("#my-service1", "MyService", "http://doesnotexi.st")))
                .build();

        // the DID document of the central issuer, e.g. a government body, etc.
        var vcIssuerDid = DidDocument.Builder.newInstance()
                .verificationMethod(List.of(VerificationMethod.Builder.create()
                        .id(CENTRAL_ISSUER_KEY_ID)
                        .type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019)
                        .publicKeyJwk(vcSigningKey.toPublicJWK().toJSONObject())
                        .build()))
                .build();

        when(didRegistryMock.resolve(eq(VP_HOLDER_ID))).thenReturn(Result.success(vpPresenterDid));
        when(didRegistryMock.resolve(eq(CENTRAL_ISSUER_DID))).thenReturn(Result.success(vcIssuerDid));
    }

    @Test
    @DisplayName("VP-JWT does not contain mandatory \"vp\" claim")
    void verifyPresentation_noVpClaim() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of());

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("VP-JWT does not contain mandatory claim: vp");
    }

    @DisplayName("VP-JWT does not contain any credential")
    @Test
    void verifyPresentation_noCredential() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted("")));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("No credential found in presentation.");
    }

    @DisplayName("VP-JWT does not contain \"verifiablePresentation\" object")
    @Test
    void verifyPresentation_invalidVpJson() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", """
                {
                    "key": "this is very invalid!"
                }
                """));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("Presentation object did not contain mandatory object: verifiableCredential");
    }

    @DisplayName("VP-JWT with a single VC-JWT - both are successfully verified")
    @Test
    void verifyPresentation_singleVc_valid() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted("\"" + vcJwt1 + "\"")));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isSucceeded();
    }

    @DisplayName("VP-JWT with a multiple VC-JWTs - all are successfully verified")
    @Test
    void verifyPresentation_multipleVc_valid() {
        // create first VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create first VC-JWT (signed by the central issuer)
        var vcJwt2 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "isoCred", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_CERTIFICATE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpContent = "\"%s\", \"%s\"".formatted(vcJwt1, vcJwt2);
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted(vpContent)));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isSucceeded();
    }

    @DisplayName("VP-JWT with one spoofed VC-JWT - expect a failure")
    @Test
    void verifyPresentation_oneVcIsInvalid() throws JOSEException {

        var spoofedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(CENTRAL_ISSUER_KEY_ID) //this bit is important for the DID resolution
                .generate();
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted("\"" + vcJwt1 + "\"")));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("Token could not be verified: Invalid signature");
    }

    @DisplayName("VP-JWT with a spoofed signature - expect a failure")
    @Test
    void verifyPresentation_vpJwtInvalid() throws JOSEException {
        var spoofedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(PRESENTER_KEY_ID) //this bit is important for the DID resolution
                .generate();
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(spoofedKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted(vcJwt1)));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("Token could not be verified: Invalid signature");
    }

    @DisplayName("VP-JWT with a missing claim - expect a failure")
    @Test
    void verifyPresentation_vpJwt_invalidClaims() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, null, MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted(vcJwt1)));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("Token could not be verified: Claim verification failed. JWT missing required claims: [sub]");
    }

    @DisplayName("VP-JWT with a VC-JWT, which misses a claim - expect a failure")
    @Test
    void verifyPresentation_vcJwt_invalidClaims() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, null, VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "test-subject", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted("\"" + vcJwt1 + "\"")));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("Token could not be verified: Claim verification failed. JWT missing required claims: [sub]");
    }

    @DisplayName("VP-JWT with a wrong audience")
    @Test
    void verifyPresentation_wrongAudience() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "test-cred-sub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "test-pres-sub", "invalid-vp-audience", Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted(vcJwt1)));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("JWT aud claim has value [invalid-vp-audience], must be [did:web:myself]");
    }

    @DisplayName("VP-JWT containing a VC-JWT, where the VC-audience does not match the VP-issuer")
    @Test
    void verifyPresentation_vcJwt_wrongAudience() {
        // create first VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE));

        // create first VC-JWT (signed by the central issuer)
        var vcJwt2 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "isoCred", "invalid-vc-audience", Map.of("vc", TestData.VC_CONTENT_FOR_JWT_CERTIFICATE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpContent = "\"%s\", \"%s\"".formatted(vcJwt1, vcJwt2);
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", TestData.VP_CONTENT_FOR_JWT.formatted(vpContent)));

        var verifier = new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(didRegistryMock), MY_OWN_DID, new ObjectMapper());
        var result = verifier.verifyPresentation(vpJwt);
        assertThat(result).isFailed().detail().contains("JWT aud claim has value [invalid-vc-audience], must be [did:web:test-issuer]");
    }

}