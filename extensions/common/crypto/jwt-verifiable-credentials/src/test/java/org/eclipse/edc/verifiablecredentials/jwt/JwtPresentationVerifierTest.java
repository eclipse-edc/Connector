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

package org.eclipse.edc.verifiablecredentials.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.identitytrust.spi.verification.VerifierContext;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier.JWT_VP_TOKEN_CONTEXT;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.MY_OWN_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.PRESENTER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VC_CONTENT_CERTIFICATE_EXAMPLE;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VC_CONTENT_DEGREE_EXAMPLE;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VP_CONTENT_TEMPLATE;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VP_HOLDER_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestFunctions.createPublicKey;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class JwtPresentationVerifierTest {

    private final PublicKeyResolver publicKeyResolverMock = mock();
    private final TokenValidationService tokenValidationService = new TokenValidationServiceImpl();
    private final TokenValidationRulesRegistry ruleRegistry = new TokenValidationRulesRegistryImpl();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JwtPresentationVerifier verifier = new JwtPresentationVerifier(typeManager, "test", tokenValidationService, ruleRegistry, publicKeyResolverMock);
    private ECKey vpSigningKey;
    private ECKey vcSigningKey;

    @BeforeEach
    void setup() throws JOSEException {
        vpSigningKey = new ECKeyGenerator(Curve.P_256)
                .keyID(VP_HOLDER_ID + "#" + PRESENTER_KEY_ID)
                .generate();

        vcSigningKey = new ECKeyGenerator(Curve.P_256)
                .keyID(CENTRAL_ISSUER_DID + "#" + CENTRAL_ISSUER_KEY_ID)
                .generate();

        var vpKeyWrapper = createPublicKey(vpSigningKey);
        when(publicKeyResolverMock.resolveKey(eq(VP_HOLDER_ID + "#" + PRESENTER_KEY_ID)))
                .thenReturn(success(vpKeyWrapper));

        var vcKeyWrapper = createPublicKey(vcSigningKey);
        when(publicKeyResolverMock.resolveKey(eq(CENTRAL_ISSUER_DID + "#" + CENTRAL_ISSUER_KEY_ID)))
                .thenReturn(success(vcKeyWrapper));


        // those rules would normally get registered in an extension
        ruleRegistry.addRule(JWT_VP_TOKEN_CONTEXT, new HasSubjectRule());

        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    @DisplayName("VP-JWT does not contain mandatory \"vp\" claim")
    void verifyPresentation_noVpClaim() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of());

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("Either 'vp' or 'vc' claim must be present in JWT.");
    }

    @DisplayName("VP-JWT does not contain any credential")
    @Test
    void verifyPresentation_noCredential() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isSucceeded();
    }

    @DisplayName("VP-JWT does not contain \"verifiablePresentation\" object")
    @Test
    void verifyPresentation_invalidVpJson() {
        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", Map.of("key", "this is very invalid!")));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("Presentation object did not contain mandatory object: verifiableCredential");
    }

    @DisplayName("VP-JWT with a single VC-JWT - both are successfully verified")
    @Test
    void verifyPresentation_singleVc_valid() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isSucceeded();
    }

    @DisplayName("VP-JWT with a multiple VC-JWTs - all are successfully verified")
    @Test
    void verifyPresentation_multipleVc_valid() {
        // create first VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create first VC-JWT (signed by the central issuer)
        var vcJwt2 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "isoCred", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_CERTIFICATE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpContent = "\"%s\", \"%s\"".formatted(vcJwt1, vcJwt2);
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isSucceeded();
    }

    @DisplayName("VP-JWT with one spoofed VC-JWT - expect a failure")
    @Test
    void verifyPresentation_oneVcIsInvalid() throws JOSEException {

        var spoofedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(CENTRAL_ISSUER_DID + "#" + CENTRAL_ISSUER_KEY_ID) //this bit is important for the DID resolution
                .generate();
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("Token verification failed");
    }

    @DisplayName("VP-JWT with a spoofed signature - expect a failure")
    @Test
    void verifyPresentation_vpJwtInvalid() throws JOSEException {
        var spoofedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(VP_HOLDER_ID + "#" + PRESENTER_KEY_ID) //this bit is important for the DID resolution
                .generate();
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(spoofedKey, VP_HOLDER_ID, "degreePres", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("Token verification failed");
    }

    @DisplayName("VP-JWT with a missing claim - expect a failure")
    @Test
    void verifyPresentation_vpJwt_invalidClaims() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, null, MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("The 'sub' claim is mandatory and must not be null.");
    }

    @DisplayName("VP-JWT with a wrong audience")
    @Test
    void verifyPresentation_wrongAudience() {
        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "test-cred-sub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "test-pres-sub", "invalid-vp-audience", Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\""))));

        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);
        assertThat(result).isFailed().detail().contains("Token audience claim (aud -> [invalid-vp-audience]) did not contain expected audience: did:web:myself");
    }

    @Test
    void verifyPresentation_jwtVpHasSpoofedKidClaim() throws JOSEException {

        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:attacker#violating-key").generate();

        when(publicKeyResolverMock.resolveKey(endsWith("violating-key"))).thenReturn(success(createPublicKey(spoofedKey.toPublicJWK())));

        // create first VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE));

        var vpContent = VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\"");


        String vpJwt;
        try {
            var signer = new ECDSASigner(spoofedKey.toECPrivateKey());

            // Prepare JWT with claims set
            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(VP_HOLDER_ID)
                    .subject("testSub")
                    .issueTime(now)
                    .audience(MY_OWN_DID)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)));

            Map.of("vp", asMap(vpContent)).forEach(claimsSet::claim);

            var signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(spoofedKey.getKeyID()).build(), claimsSet.build());

            signedJwt.sign(signer);

            vpJwt = signedJwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vpJwt, context);

        assertThat(result)
                .isFailed()
                .detail()
                .isEqualTo("kid header 'did:web:attacker#violating-key' expected to correlate to 'iss' claim ('did:web:test-issuer'), but it did not.");
    }

    @Test
    void verifyCredential_jwtVpHasSpoofedKidClaim() throws JOSEException {

        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:attacker#violating-key").generate();

        when(publicKeyResolverMock.resolveKey(endsWith("violating-key"))).thenReturn(success(createPublicKey(spoofedKey.toPublicJWK())));

        // create first VC-JWT (signed by the central issuer)

        String vcJwt1;
        try {
            var signer = new ECDSASigner(spoofedKey.toECPrivateKey());

            // Prepare JWT with claims set
            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(CENTRAL_ISSUER_DID)
                    .subject("degreeSub")
                    .issueTime(now)
                    .audience(VP_HOLDER_ID)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)));

            Map.of("vc", VC_CONTENT_DEGREE_EXAMPLE).forEach(claimsSet::claim);

            var signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(spoofedKey.getKeyID()).build(), claimsSet.build());

            signedJwt.sign(signer);

            vcJwt1 = signedJwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        var context = VerifierContext.Builder.newInstance()
                .verifier(verifier)
                .audience(MY_OWN_DID)
                .build();
        var result = verifier.verify(vcJwt1, context);

        assertThat(result)
                .isFailed()
                .detail()
                .isEqualTo("kid header 'did:web:attacker#violating-key' expected to correlate to 'iss' claim ('did:web:some-official-issuer'), but it did not.");
    }


    private Map<String, Object> asMap(String rawContent) {
        try {
            return mapper.readValue(rawContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}