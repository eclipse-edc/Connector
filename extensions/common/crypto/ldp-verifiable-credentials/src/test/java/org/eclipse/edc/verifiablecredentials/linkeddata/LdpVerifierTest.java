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

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.method.resolver.MethodResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.VerifierContext;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;
import org.eclipse.edc.security.signature.jws2020.Jws2020ProofDraft;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.security.signature.jws2020.TestDocumentLoader;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.createKeyPair;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.MEMBERSHIP_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.NAME_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.VC_CONTENT_CERTIFICATE_EXAMPLE;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createMembershipCredential;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createNameCredential;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdpVerifierTest {

    private static final String VP_HOLDER = "did:web:vp-holder";
    private final ObjectMapper mapper = createObjectMapper();
    private final TestDocumentLoader testDocLoader = new TestDocumentLoader("https://org.eclipse.edc/", "", SchemeRouter.defaultInstance());
    private final MethodResolver mockDidResolver = mock();

    private final SignatureSuiteRegistry suiteRegistry = mock();
    private final TypeManager typeManager = mock();
    private VerifierContext context = null;
    private LdpVerifier ldpVerifier;
    private TitaniumJsonLd jsonLd;

    @Nested
    class JsonWebSignature2020 {

        private final Jws2020SignatureSuite jwsSignatureSuite = new Jws2020SignatureSuite(mapper);

        @BeforeEach
        void setUp() throws URISyntaxException {
            jsonLd = new TitaniumJsonLd(mock());
            jsonLd.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", Thread.currentThread().getContextClassLoader().getResource("odrl.jsonld").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/ns/did/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
            jsonLd.registerCachedDocument("https://w3id.org/security/suites/jws-2020/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/v1", Thread.currentThread().getContextClassLoader().getResource("credentials.v1.json").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", Thread.currentThread().getContextClassLoader().getResource("examples.v1.json").toURI());
            ldpVerifier = LdpVerifier.Builder.newInstance()
                    .signatureSuites(suiteRegistry)
                    .jsonLd(jsonLd)
                    .typeManager(typeManager)
                    .typeContext("test")
                    .methodResolvers(List.of(mockDidResolver))
                    .loader(testDocLoader)
                    .build();
            context = VerifierContext.Builder.newInstance().verifier(ldpVerifier).build();

            when(suiteRegistry.getAllSuites()).thenReturn(List.of(jwsSignatureSuite));
            when(typeManager.getMapper("test")).thenReturn(mapper);
        }

        private Jws2020ProofDraft generateEmbeddedProof(VerificationMethod verificationMethod) {
            return proofBuilder(verificationMethod)
                    .build();
        }

        private Jws2020ProofDraft.Builder proofBuilder(VerificationMethod verificationMethod) {
            return Jws2020ProofDraft.Builder.newInstance()
                    .mapper(mapper)
                    .created(Instant.now())
                    .verificationMethod(verificationMethod)
                    .proofPurpose(URI.create("https://w3id.org/security#assertionMethod"));
        }

        @Nested
        class Presentations {
            @Test
            void verify_noCredentials_success() throws JOSEException {
                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");

                var verificationMethod = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(verificationMethod), testDocLoader);

                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();
            }

            @Test
            void verify_singleValidCredentials_success() throws JOSEException {
                // create signed VC
                var vcKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key")
                        .generate();
                var vcVerificationMethod = createKeyPair(vcKey, "did:web:test-issuer");
                var rawVc = LdpCreationUtils.signDocument(VC_CONTENT_CERTIFICATE_EXAMPLE, vcKey, generateEmbeddedProof(vcVerificationMethod), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(rawVc);

                var vpVerificationMethod = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVerificationMethod), testDocLoader);

                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();

            }

            @Test
            void verify_multipleValidCredentials_success() throws JOSEException {
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var nameVm = createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var memberVm = createKeyPair(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(memberVm), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVerificationMethod = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVerificationMethod), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();
            }

            @Test
            void verify_singleInvalidVc_shouldFail() throws JOSEException {
                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var vm1 = createKeyPair(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(vm1), testDocLoader);

                // create name credential, but altered after-the-fact
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var vm2 = createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(vm2), testDocLoader);
                // now change the name
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVm = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVm), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");

            }

            @Test
            void verify_multipleInvalidVc_shouldFail() throws JOSEException {
                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var verificationMethod = createKeyPair(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(verificationMethod), testDocLoader);
                // tamper with the status -> causes signature failure!
                signedMembershipCred = signedMembershipCred.replace("active", "super-active");

                // create name credential, but altered after-the-fact
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var nameVm = createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);
                // tamper with the name -> causes signature failure!
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVm = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVm), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_forgedPresentation_shouldFail() throws JOSEException {
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var nameVm = createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var memberVm = createKeyPair(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(memberVm), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);


                var vpVm = createKeyPair(vpKey, VP_HOLDER);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVm), testDocLoader);
                // tamper with the presentation
                rawVp = rawVp.replace("\"https://holder.test.com\"", "\"https://another-holder.test.com\"");
                // modify
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_proofPurposeNotAssertionMethod() throws JOSEException {
                // create signed VP, that contains the VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER));
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey,
                        proofOptions.proofPurpose(URI.create("https://test.org/notValid")).build(), testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("InvalidProofPurpose");
            }
        }

        @Nested
        class Credentials {
            @Test
            void verify_success() throws JOSEException {
                var signingKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var verificationMethod = new JsonWebKeyPair(URI.create(TestData.NAME_CREDENTIAL_ISSUER), URI.create("https://w3id.org/security#JsonWebKey2020"), null, signingKey);

                var proofOptions = proofBuilder(verificationMethod)
                        .build();

                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, verificationMethod, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isSucceeded();
            }

            @Test
            void verify_forgedProof_shouldFail() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var forgedKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();

                var proofKey = new JsonWebKeyPair(URI.create(TestData.NAME_CREDENTIAL_ISSUER), URI.create("https://w3id.org/security#JsonWebKey2020"), null, nameKey);
                var proofDraft = proofBuilder(proofKey).build();

                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, forgedKey, proofDraft, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_noProof_fails() throws JsonProcessingException {
                var credential = jsonLd.expand(mapper.readValue(VC_CONTENT_CERTIFICATE_EXAMPLE, JsonObject.class)).getContent().toString();
                assertThat(ldpVerifier.verify(credential, context)).isFailed().detail().contains("MissingProof");
            }

            @Test
            void verify_proofPurposeNotAssertionMethod() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER)).proofPurpose(URI.create("https://test.org/notValid")).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("InvalidProofPurpose");
            }

            @Test
            void verify_verificationMethodIsDid() throws JOSEException, DocumentError {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var identifier = URI.create("did:web:test-issuer");
                var nameCredential = createNameCredential(identifier.toString());
                var did = new JsonWebKeyPair(identifier, null, null, null);
                ArgumentMatcher<URI> uriMatcher = argument -> argument.equals(identifier);

                when(mockDidResolver.isAccepted(argThat(uriMatcher))).thenReturn(true);
                when(mockDidResolver.resolve(argThat(uriMatcher), any(), any())).thenReturn(new JsonWebKeyPair(identifier, null, null, nameKey));

                var proofOptions = proofBuilder(createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER)).verificationMethod(did).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isSucceeded();
            }

            @Test
            void verify_verificationMethodIsDid_doesNotMatchIssuer() throws JOSEException, DocumentError {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var identifier = URI.create("did:web:test-issuer");
                var nameCredential = createNameCredential("did:web:some-other-issuer");
                var did = new JsonWebKeyPair(identifier, null, null, null);
                ArgumentMatcher<URI> uriMatcher = argument -> argument.equals(identifier);

                when(mockDidResolver.isAccepted(argThat(uriMatcher))).thenReturn(true);
                when(mockDidResolver.resolve(argThat(uriMatcher), any(), any())).thenReturn(new JsonWebKeyPair(identifier, null, null, nameKey));

                var proofOptions = proofBuilder(createKeyPair(nameKey, NAME_CREDENTIAL_ISSUER)).verificationMethod(did).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Issuer and proof.verificationMethod mismatch");
            }


        }
    }

    @Nested
    class Ed25519Signature2018 {
        // not yet implemented
    }
}