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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.identitytrust.service.verification;

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.vc.issuer.ProofDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.security.signature.jws2020.Jws2020ProofDraft;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.security.signature.jws2020.TestDocumentLoader;
import org.eclipse.edc.security.signature.jws2020.TestFunctions;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpCreationUtils;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.eclipse.edc.verifiablecredentials.linkeddata.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.MY_OWN_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.PRESENTER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VP_HOLDER_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestFunctions.createPublicKey;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.MEMBERSHIP_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.NAME_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.VP_CONTENT_TEMPLATE;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createMembershipCredential;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createNameCredential;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MultiFormatPresentationVerifierTest {
    public static final String INVALID_SIGNATURE = "Invalid signature";
    private static final SignatureSuiteRegistry SIGNATURE_SUITE_REGISTRY = mock();
    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private static final Jws2020SignatureSuite JWS_SIGNATURE_SUITE = new Jws2020SignatureSuite(MAPPER);
    private static ECKey vpSigningKey;
    private static ECKey vcSigningKey;
    private static TitaniumJsonLd jsonLd;
    private final TypeManager typeManager = mock();
    private final PublicKeyResolver publicKeyResolverMock = mock();
    private final TestDocumentLoader testDocLoader = new TestDocumentLoader("https://org.eclipse.edc/", "", SchemeRouter.defaultInstance());
    private final TokenValidationService tokenValidationService = mock();
    private MultiFormatPresentationVerifier multiFormatVerifier;

    @BeforeAll
    static void prepare() throws URISyntaxException, JOSEException {
        when(SIGNATURE_SUITE_REGISTRY.getForId(any())).thenReturn(JWS_SIGNATURE_SUITE);
        when(SIGNATURE_SUITE_REGISTRY.getAllSuites()).thenReturn(List.of(JWS_SIGNATURE_SUITE));
        jsonLd = new TitaniumJsonLd(mock());
        jsonLd.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", Thread.currentThread().getContextClassLoader().getResource("odrl.jsonld").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/ns/did/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
        jsonLd.registerCachedDocument("https://w3id.org/security/suites/jws-2020/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/v1", Thread.currentThread().getContextClassLoader().getResource("credentials.v1.json").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", Thread.currentThread().getContextClassLoader().getResource("examples.v1.json").toURI());

        vpSigningKey = new ECKeyGenerator(Curve.P_256).keyID(PRESENTER_KEY_ID).generate();
        vcSigningKey = new ECKeyGenerator(Curve.P_256).keyID(CENTRAL_ISSUER_KEY_ID).generate();
    }

    @BeforeEach
    void setup() {
        when(publicKeyResolverMock.resolveKey(endsWith(PRESENTER_KEY_ID))).thenReturn(success(createPublicKey(vpSigningKey.toPublicJWK())));
        when(publicKeyResolverMock.resolveKey(endsWith(CENTRAL_ISSUER_KEY_ID))).thenReturn(success(createPublicKey(vcSigningKey.toPublicJWK())));

        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                .thenReturn(Result.success(null));


        var ldpVerifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(SIGNATURE_SUITE_REGISTRY)
                .jsonLd(jsonLd)
                .typeManager(typeManager)
                .typeContext("test")
                .build();

        var jwtPresentationVerifier = new JwtPresentationVerifier(typeManager, "test", tokenValidationService, mock(), publicKeyResolverMock);
        multiFormatVerifier = new MultiFormatPresentationVerifier(MY_OWN_DID, jwtPresentationVerifier, ldpVerifier);
        when(typeManager.getMapper("test")).thenReturn(MAPPER);
    }

    private ProofDraft generateEmbeddedProofOptions(ECKey vcKey, String proofPurpose) {

        return Jws2020ProofDraft.Builder.newInstance()
                .mapper(MAPPER)
                .created(Instant.now())
                .verificationMethod(TestFunctions.createKeyPair(vcKey, proofPurpose)) // embedded proof
                .proofPurpose(URI.create("https://w3id.org/security#assertionMethod"))
                .build();
    }

    private Map<String, Object> asMap(String rawContent) {
        try {
            return MAPPER.readValue(rawContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class JwtVp {

        @DisplayName("contains only JWT-VC (single)")
        @Test
        void verify_hasJwtVc_success() {
            // create first VC-JWT (signed by the central issuer)
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var vpContent = VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\"");
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(vpContent)));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

        @DisplayName("contains only JWT-VCs (multiple)")
        @Test
        void verify_hasJwtVcs_success() {
            // create first VC-JWT (signed by the central issuer)
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            // create first VC-JWT (signed by the central issuer)
            var vcJwt2 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "isoCred", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_CERTIFICATE_EXAMPLE));

            // create VP-JWT (signed by the presenter) that contains the VP as a claim
            var vpContent = "\"%s\", \"%s\"".formatted(vcJwt1, vcJwt2);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

        @DisplayName("contains only LDP-VC (single)")
        @Test
        void verify_hasLdpVc_success() {
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = VP_CONTENT_TEMPLATE.formatted(signedNameCredential);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(vpContent)));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

        @DisplayName("contains only LDP-VCs (multiple)")
        @Test
        void verify_hasLdpVcs_success() {

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);


            var vpContent = "%s, %s".formatted(signedMemberCredential, signedNameCredential);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

        @DisplayName("containing both LDP-VC and JWT-VC")
        @Test
        void verify_mixedVcs_success() {
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = "%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

        @DisplayName("contains only one forged JWT-VC (single)")
        @Test
        void verify_hasJwtVc_forged_fails() throws JOSEException {
            // during DID resolution, the "vcSigningKey" would be resolved, which is different from the "spoofedKey"
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(CENTRAL_ISSUER_KEY_ID).generate();
            var vcJwt1 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            when(tokenValidationService.validate(eq(vcJwt1), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.failure("Invalid signature"));

            var vpContent = VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\"");
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(vpContent)));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains(INVALID_SIGNATURE);
        }

        @DisplayName("contains only JWT-VCs, one is forged ")
        @Test
        void verify_hasJwtVcs_forged_success() throws JOSEException {
            // during DID resolution, the "vcSigningKey" would be resolved, which is different from the "spoofedKey"
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(CENTRAL_ISSUER_KEY_ID).generate();
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            // create first VC-JWT (signed by the central issuer)
            var vcJwt2 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "isoCred", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_CERTIFICATE_EXAMPLE));

            when(tokenValidationService.validate(eq(vcJwt2), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.failure("Invalid signature"));

            // create VP-JWT (signed by the presenter) that contains the VP as a claim
            var vpContent = "\"%s\", \"%s\"".formatted(vcJwt1, vcJwt2);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains(INVALID_SIGNATURE);
        }

        @DisplayName("contains only one forged LDP-VC")
        @Test
        void verify_hasLdpVc_forged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, NAME_CREDENTIAL_ISSUER), testDocLoader);


            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(signedNameCredential))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("contains only LDP-VCs, one is forged")
        @Test
        void verify_hasLdpVcs_forged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);


            var vpContent = "%s, %s".formatted(signedMemberCredential, signedNameCredential);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("containing both LDP-VC and JWT-VC, the LDP-VC is forged")
        @Test
        void verify_mixedVcs_ldpForged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = "%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("containing both LDP-VC and JWT-VC, the JWT-VC is forged")
        @Test
        void verify_mixedVcs_jwtForged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(CENTRAL_ISSUER_KEY_ID).generate();

            var vcJwt1 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));
            when(tokenValidationService.validate(eq(vcJwt1), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.failure("Invalid signature"));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = "%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1);
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", asMap(VP_CONTENT_TEMPLATE.formatted(vpContent))));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null)))
                    .isFailed().detail().contains("Invalid signature");
        }

        @DisplayName("contains no VCs")
        @Test
        void verify_noCredentials() {
            // create first VC-JWT (signed by the central issuer)

            var vpContent = asMap(VP_CONTENT_TEMPLATE.formatted(""));
            var vpJwt = JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", vpContent));

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }

    }

    /**
     * As per their <a href="https://www.w3.org/2018/credentials/v1">schema</a>, ldp_vp's can only contain ldp_vc's. The VerifiableCredentials Data Model
     * specification does not make that distinction though. The Multiformat verifier could handle the case, even if it's (currently) not possible.
     */
    @Nested
    class LdpVp {
        @DisplayName("contains only JWT-VC (single), which is stripped out by the expansion")
        @Test
        void verify_hasJwtVc_succeeds() {
            // create first VC-JWT (signed by the central issuer)
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var vpContent = VP_CONTENT_TEMPLATE.formatted("\"" + vcJwt1 + "\"");
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("contains only LDP-VC (single)")
        @Test
        void verify_hasLdpVc_success() {
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = VP_CONTENT_TEMPLATE.formatted(signedNameCredential);

            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null))).isSucceeded();
        }

        @DisplayName("contains only LDP-VCs (multiple)")
        @Test
        void verify_hasLdpVcs_success() {

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);


            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s, %s".formatted(signedMemberCredential, signedNameCredential));
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null))).isSucceeded();
        }

        @DisplayName("containing both LDP-VC and JWT-VC, JWT gets stripped out during expansion")
        @Test
        void verify_mixedVcs_success() {
            var spy = Mockito.spy(multiFormatVerifier.getContext().getVerifiers().stream().filter(cv -> cv instanceof JwtPresentationVerifier).findFirst().get());
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1));
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
            verifyNoInteractions(spy);
        }

        @DisplayName("contains only one forged LDP-VC")
        @Test
        void verify_hasLdpVc_forged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s, %s".formatted(signedNameCredential, signedNameCredential));
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("contains only LDP-VCs, one is forged")
        @Test
        void verify_hasLdpVcs_forged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);


            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s, %s".formatted(signedMemberCredential, signedNameCredential));
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("containing both LDP-VC and JWT-VC, the LDP-VC is forged")
        @Test
        void verify_mixedVcs_ldpForged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_384).keyID("violating-key").generate();
            var vcJwt1 = JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(spoofedKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = "%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1);
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("containing both LDP-VC and JWT-VC, the JWT-VC is forged")
        @Test
        void verify_mixedVcs_jwtForged_fails() throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(CENTRAL_ISSUER_KEY_ID).generate();

            var vcJwt1 = JwtCreationUtils.createJwt(spoofedKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));

            var nameCredential = createNameCredential();
            var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

            var memberCredential = createMembershipCredential();
            var signedMemberCredential = LdpCreationUtils.signDocument(memberCredential, vcSigningKey, generateEmbeddedProofOptions(vcSigningKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s, %s, \"%s\"".formatted(signedMemberCredential, signedNameCredential, vcJwt1));
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null)))
                    .isFailed().detail().contains("InvalidSignature");
        }

        @DisplayName("contains no VCs")
        @Test
        void verify_noCredentials() {
            // create first VC-JWT (signed by the central issuer)

            var vpContent = VP_CONTENT_TEMPLATE.formatted("");
            var vpLdp = LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);

            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpLdp, CredentialFormat.VC1_0_LD, null))).isSucceeded();
        }

    }

}