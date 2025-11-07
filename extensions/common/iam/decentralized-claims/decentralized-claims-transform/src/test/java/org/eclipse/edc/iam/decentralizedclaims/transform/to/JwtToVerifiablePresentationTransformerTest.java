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

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestData;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VP;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VP_EMPTY_CREDENTIALS_ARRAY;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VP_SINGLE_VC;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VP_WITH_LDP_VC;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXMPLE_JWT_VP_NO_VP_CLAIM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtToVerifiablePresentationTransformerTest {

    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final TransformerContext context = mock();
    private final Monitor monitor = mock();
    private final JwtToVerifiableCredentialTransformer credentialTransformer = new JwtToVerifiableCredentialTransformer(monitor);
    private final JsonLd jsonLd = mock();
    private final JwtToVerifiablePresentationTransformer transformer = new JwtToVerifiablePresentationTransformer(monitor, typeManager, "test", jsonLd);

    @BeforeEach
    void setup() {
        when(context.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenAnswer(a -> credentialTransformer.transform(a.getArgument(0), context));

        when(typeManager.getMapper("test")).thenReturn(MAPPER);
    }

    @Test
    void transform_success() {
        var vp = transformer.transform(EXAMPLE_JWT_VP, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation", "CredentialManagerPresentation");
        assertThat(vp.getCredentials()).hasSize(1).allSatisfy(vc -> {
            assertThat(vc.getCredentialSubject()).isNotEmpty();
            assertThat(vc.getType()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("VP has a single 'verifiableCredential' (not an array)")
    void transform_success_singleCredential() {
        var vp = transformer.transform(EXAMPLE_JWT_VP_SINGLE_VC, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation", "CredentialManagerPresentation");
        assertThat(vp.getCredentials()).hasSize(1)
                .doesNotContainNull()
                .allSatisfy(vc -> {
                    assertThat(vc.getCredentialSubject()).isNotEmpty();
                    assertThat(vc.getType()).isNotEmpty();
                });
    }

    @Test
    @DisplayName("JWT does not contain a 'vp' claim")
    void transform_noVpClaim() {
        var vp = transformer.transform(EXMPLE_JWT_VP_NO_VP_CLAIM, context);
        assertThat(vp).isNull();
        verify(context).reportProblem(eq("Could not parse VerifiablePresentation from JWT."));
    }


    @Test
    @DisplayName("VP claim contains an empty 'verifiableCredentials' array")
    void transform_vpClaimWithoutCredentials() {
        var vp = transformer.transform(EXAMPLE_JWT_VP_EMPTY_CREDENTIALS_ARRAY, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation", "CredentialManagerPresentation");
        assertThat(vp.getCredentials()).isEmpty();
    }

    @Test
    @DisplayName("VP claim contains a single LDP-VC")
    void transform_containsLdpVc() throws JsonProcessingException {
        when(jsonLd.expand(any()))
                .thenReturn(Result.success(JacksonJsonLd.createObjectMapper()
                        .readValue(TestUtils.getResourceFileContentAsString("expanded_vc.json"), JsonObject.class)));

        when(context.transform(isA(JsonObject.class), eq(VerifiableCredential.class)))
                .thenAnswer(a -> VerifiableCredential.Builder.newInstance().type("VerifiableCredential")
                        .credentialSubject(CredentialSubject.Builder.newInstance().id("test-subj").claim("key", "val").build())
                        .issuer(new Issuer("test-issuer", Map.of()))
                        .issuanceDate(Instant.now())
                        .build());


        var vp = transformer.transform(EXAMPLE_JWT_VP_WITH_LDP_VC, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation", "CredentialManagerPresentation");
        assertThat(vp.getCredentials()).hasSize(1)
                .doesNotContainNull()
                .allSatisfy(vc -> {
                    assertThat(vc.getCredentialSubject()).isNotEmpty();
                    assertThat(vc.getType()).isNotEmpty();
                });
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    @DisplayName("String is not a valid JWT")
    void transform_inputIsNotValidJwt() {
        assertThat(transformer.transform("foobar", context)).isNull();
        verify(context).reportProblem("Could not parse VerifiablePresentation from JWT.");
    }

    @DisplayName("VP is an EnvelopedVerifiablePresentation")
    @Test
    void transform_envelopedPresentation() {
        var vp = transformer.transform(TestData.EXAMPLE_ENVELOPED_PRESENTATION, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation");
        assertThat(vp.getCredentials()).hasSize(2)
                .allSatisfy(vc -> {
                    assertThat(vc.getCredentialSubject()).isNotEmpty();
                    assertThat(vc.getType()).containsExactlyInAnyOrder("ExampleDegreeCredential", "ExamplePersonCredential", "VerifiableCredential");
                });
        verify(context, never()).reportProblem(anyString());
    }

    @DisplayName("VP is EnvelopedVerifiablePresentation, VC is not enveloped")
    @Test
    void transform_envelopedPresentation_vcNotEnveloped() {
        var vp = transformer.transform(TestData.EXAMPLE_ENVELOPED_PRESENTATION_VC_NOT_ENVELOPED, context);
        assertThat(vp).isNotNull();
        assertThat(vp.getTypes()).containsExactlyInAnyOrder("VerifiablePresentation");
        assertThat(vp.getCredentials()).isEmpty();
        verify(context, atLeastOnce()).reportProblem(eq("Credential object is not a valid EnvelopedVerifiableCredential"));
    }
}