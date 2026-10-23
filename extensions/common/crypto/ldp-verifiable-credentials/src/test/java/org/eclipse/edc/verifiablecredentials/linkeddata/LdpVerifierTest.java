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
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.method.resolver.MethodResolver;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.json.JsonMapperFactory;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;
import org.eclipse.edc.security.signature.jws2020.Jws2020ProofDraft;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.security.signature.jws2020.TestDocumentLoader;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.createKeyPair;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.MEMBERSHIP_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.NAME_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.VC_CONTENT_CERTIFICATE_EXAMPLE;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createMembershipCredential;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createNameCredential;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LdpVerifierTest {

    private static final String VP_HOLDER = "did:web:test-issuer";
    private final ObjectMapper mapper = JsonMapperFactory.defaultJsonMapper();
    private final TestDocumentLoader testDocLoader = new TestDocumentLoader("https://org.eclipse.edc/", "", SchemeRouter.defaultInstance());
    private final TestMethodResolver methodResolver = new TestMethodResolver();

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
            ldpVerifier = verifierBuilder().build();
            context = VerifierContext.Builder.newInstance().verifier(ldpVerifier).build();

            when(suiteRegistry.getAllSuites()).thenReturn(List.of(jwsSignatureSuite));
            when(typeManager.getMapper("test")).thenReturn(mapper);
        }

        private LdpVerifier.Builder verifierBuilder() {
            return LdpVerifier.Builder.newInstance()
                    .signatureSuites(suiteRegistry)
                    .jsonLd(jsonLd)
                    .typeManager(typeManager)
                    .typeContext("test")
                    .methodResolvers(List.of(methodResolver))
                    .loader(testDocLoader);
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

                var verificationMethod = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
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
                var vcVerificationMethod = methodResolver.publish("did:web:test-issuer#vc-sign-key", vcKey);
                var rawVc = LdpCreationUtils.signDocument(VC_CONTENT_CERTIFICATE_EXAMPLE, vcKey, generateEmbeddedProof(vcVerificationMethod), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(rawVc);

                var vpVerificationMethod = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
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
                var nameVm = methodResolver.publish(NAME_CREDENTIAL_ISSUER + "#key-1", nameKey);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var memberVm = methodResolver.publish(MEMBERSHIP_CREDENTIAL_ISSUER + "#key-1", membershipKey);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(memberVm), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVerificationMethod = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
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
                var vm1 = methodResolver.publish(MEMBERSHIP_CREDENTIAL_ISSUER + "#key-1", membershipKey);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(vm1), testDocLoader);

                // create name credential, but altered after-the-fact
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var vm2 = methodResolver.publish(NAME_CREDENTIAL_ISSUER + "#key-1", nameKey);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(vm2), testDocLoader);
                // now change the name
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVm = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
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
                var verificationMethod = methodResolver.publish(MEMBERSHIP_CREDENTIAL_ISSUER + "#key-1", membershipKey);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(verificationMethod), testDocLoader);
                // tamper with the status -> causes signature failure!
                signedMembershipCred = signedMembershipCred.replace("active", "super-active");

                // create name credential, but altered after-the-fact
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var nameVm = methodResolver.publish(NAME_CREDENTIAL_ISSUER + "#key-1", nameKey);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);
                // tamper with the name -> causes signature failure!
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var vpVm = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
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
                var nameVm = methodResolver.publish(NAME_CREDENTIAL_ISSUER + "#key-1", nameKey);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProof(nameVm), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var memberVm = methodResolver.publish(MEMBERSHIP_CREDENTIAL_ISSUER + "#key-1", membershipKey);
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProof(memberVm), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);


                var vpVm = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(vpVm), testDocLoader);
                // tamper with the presentation
                rawVp = rawVp.replace("\"https://exapmle.com/test-vp\"", "\"https://exapmle.com/another-vp\"");
                // modify
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_proofPurposeAuthentication_success() throws JOSEException {
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");

                var proof = proofBuilder(methodResolver.publish(VP_HOLDER + "#key-1", vpKey))
                        .proofPurpose(URI.create("https://w3id.org/security#authentication"))
                        .build();
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, proof, testDocLoader);

                assertThat(ldpVerifier.verify(rawVp, context)).isSucceeded();
            }

            @Test
            void verify_proofPurposeInvalid_shouldFail() throws JOSEException {
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");

                var proof = proofBuilder(methodResolver.publish(VP_HOLDER + "#key-1", vpKey))
                        .proofPurpose(URI.create("https://w3id.org/security#capabilityInvocation"))
                        .build();
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, proof, testDocLoader);

                assertThat(ldpVerifier.verify(rawVp, context)).isFailed()
                        .detail().contains("Invalid proof purpose 'https://w3id.org/security#capabilityInvocation'");
            }

            @Test
            void verify_signedByOtherDid_shouldFail() throws JOSEException {
                // a key of another DID, published in that DID's document, signs a presentation for the holder
                var attackerKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("attacker-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");

                var verificationMethod = methodResolver.publish("did:web:attacker#key-1", attackerKey);
                var rawVp = LdpCreationUtils.signDocument(input, attackerKey, generateEmbeddedProof(verificationMethod), testDocLoader);

                assertThat(ldpVerifier.verify(rawVp, context)).isFailed()
                        .detail().contains("Holder and proof.verificationMethod mismatch");
            }

            @Test
            void verify_noHolder_shouldFail() throws JOSEException {
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("").replace("\"holder\": \"%s\",".formatted(VP_HOLDER), "");

                var verificationMethod = methodResolver.publish(VP_HOLDER + "#key-1", vpKey);
                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProof(verificationMethod), testDocLoader);

                assertThat(ldpVerifier.verify(rawVp, context)).isFailed()
                        .detail().contains("Presentation must contain a 'holder' property");
            }

            @Test
            void verify_credentialProofPurposeNotAssertionMethod() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(methodResolver.publish(NAME_CREDENTIAL_ISSUER + "#key-1", nameKey));
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey,
                        proofOptions.proofPurpose(URI.create("https://test.org/notValid")).build(), testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Invalid proof purpose 'https://test.org/notValid'");
            }
        }

        @Nested
        class PresentationBinding {
            private static final String AUDIENCE = "did:web:verifier";
            private final VerifierContext audienceContext = VerifierContext.Builder.newInstance().audience(AUDIENCE).build();

            @Test
            void verify_matchingDomainAndNotExpired_success() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.domain(AUDIENCE).challenge("test-challenge").expires(Instant.now().plusSeconds(60)));

                assertThat(verifierBuilder().requireBinding(true).build().verify(rawVp, audienceContext)).isSucceeded();
            }

            @Test
            void verify_domainMismatch_shouldFail() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.domain("did:web:other"));

                assertThat(ldpVerifier.verify(rawVp, audienceContext)).isFailed()
                        .detail().contains("does not match the expected audience");
            }

            @Test
            void verify_expired_shouldFail() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.domain(AUDIENCE).expires(Instant.now().minusSeconds(60)));

                assertThat(ldpVerifier.verify(rawVp, audienceContext)).isFailed()
                        .detail().contains("Presentation proof has expired");
            }

            @Test
            void verify_domainTampered_shouldFail() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.domain("did:web:other"));

                var tampered = rawVp.replace("did:web:other", AUDIENCE);

                assertThat(ldpVerifier.verify(tampered, audienceContext)).isFailed();
            }

            @Test
            void verify_noDomainAndExpires_notRequired_success() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof);

                assertThat(ldpVerifier.verify(rawVp, audienceContext)).isSucceeded();
            }

            @Test
            void verify_noDomain_required_shouldFail() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.expires(Instant.now().plusSeconds(60)));

                assertThat(verifierBuilder().requireBinding(true).build().verify(rawVp, audienceContext)).isFailed()
                        .detail().contains("Presentation proof has no 'domain'");
            }

            @Test
            void verify_noExpires_required_shouldFail() throws JOSEException {
                var rawVp = signedPresentation(proof -> proof.domain(AUDIENCE));

                assertThat(verifierBuilder().requireBinding(true).build().verify(rawVp, audienceContext)).isFailed()
                        .detail().contains("Presentation proof has no 'expires'");
            }

            private String signedPresentation(UnaryOperator<Jws2020ProofDraft.Builder> proofOptions) throws JOSEException {
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");
                var proof = proofOptions.apply(proofBuilder(methodResolver.publish(VP_HOLDER + "#key-1", vpKey))).build();
                return LdpCreationUtils.signDocument(input, vpKey, proof, testDocLoader);
            }
        }

        @Nested
        class Credentials {
            private static final String NAME_ISSUER_KEY = NAME_CREDENTIAL_ISSUER + "#key-1";

            @Test
            void verify_success() throws JOSEException {
                var signingKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(methodResolver.publish(NAME_ISSUER_KEY, signingKey)).build();

                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, signingKey, proofOptions, testDocLoader);

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
                var proofDraft = proofBuilder(methodResolver.publish(NAME_ISSUER_KEY, nameKey)).build();

                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, forgedKey, proofDraft, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_embeddedKeyOfOtherParty_shouldFail() throws JOSEException {
                var issuerKey = new ECKeyGenerator(Curve.P_256).generate();
                var attackerKey = new ECKeyGenerator(Curve.P_256).generate();
                methodResolver.publish(NAME_ISSUER_KEY, issuerKey);

                var nameCredential = createNameCredential();
                var proofDraft = proofBuilder(createKeyPair(attackerKey, NAME_ISSUER_KEY)).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, attackerKey, proofDraft, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_embeddedKeyNotResolvable_shouldFail() throws JOSEException {
                var attackerKey = new ECKeyGenerator(Curve.P_256).generate();

                var nameCredential = createNameCredential();
                var proofDraft = proofBuilder(createKeyPair(attackerKey, NAME_ISSUER_KEY)).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, attackerKey, proofDraft, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed().detail().contains("ProofVerificationMethod");
            }

            @Test
            void verify_verificationMethodOfOtherIssuer_shouldFail() throws JOSEException {
                var attackerKey = new ECKeyGenerator(Curve.P_256).generate();

                var nameCredential = createNameCredential();
                var proofDraft = proofBuilder(methodResolver.publish("did:web:attacker#key-1", attackerKey)).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, attackerKey, proofDraft, testDocLoader);
                MethodResolver resolver = mock();

                assertThat(verifierBuilder().methodResolvers(List.of(resolver)).build().verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Issuer and proof.verificationMethod mismatch");
                verifyNoInteractions(resolver);
            }

            @Test
            void verify_resolvedMethodHasOtherId_shouldFail() throws JOSEException {
                var attackerKey = new ECKeyGenerator(Curve.P_256).generate();
                var reference = methodResolver.publish(NAME_ISSUER_KEY, attackerKey);
                // e.g. a document fetched from the method URL that lists a method with another ID
                methodResolver.register(NAME_ISSUER_KEY, new JsonWebKeyPair(URI.create(NAME_CREDENTIAL_ISSUER + "#key-2"),
                        URI.create("https://w3id.org/security#JsonWebKey2020"), null, attackerKey.toPublicJWK()));

                var nameCredential = createNameCredential();
                var proofDraft = proofBuilder(reference).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, attackerKey, proofDraft, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Resolved verification method does not match");
            }

            @Test
            void verify_noProof_fails() throws JacksonException {
                var credential = jsonLd.expand(mapper.readValue(VC_CONTENT_CERTIFICATE_EXAMPLE, JsonObject.class)).getContent().toString();
                assertThat(ldpVerifier.verify(credential, context)).isFailed().detail().contains("MissingProof");
            }

            @Test
            void verify_proofPurposeAuthentication_shouldFail() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(methodResolver.publish(NAME_ISSUER_KEY, nameKey)).proofPurpose(URI.create("https://w3id.org/security#authentication")).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Invalid proof purpose 'https://w3id.org/security#authentication'");
            }

            @Test
            void verify_proofPurposeNotAssertionMethod() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = proofBuilder(methodResolver.publish(NAME_ISSUER_KEY, nameKey)).proofPurpose(URI.create("https://test.org/notValid")).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Invalid proof purpose 'https://test.org/notValid'");
            }

            @Test
            void verify_verificationMethodIsDid() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var identifier = "did:web:test-issuer";
                var nameCredential = createNameCredential(identifier);

                var proofOptions = proofBuilder(methodResolver.publish(identifier, nameKey)).build();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isSucceeded();
            }

            @Test
            void verify_verificationMethodIsDid_doesNotMatchIssuer() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential("did:web:some-other-issuer");

                var proofOptions = proofBuilder(methodResolver.publish("did:web:test-issuer", nameKey)).build();
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