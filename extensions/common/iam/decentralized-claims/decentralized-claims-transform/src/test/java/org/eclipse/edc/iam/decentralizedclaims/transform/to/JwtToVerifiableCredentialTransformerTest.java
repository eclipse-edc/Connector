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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestData;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VC;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VC_NO_DATES;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.EXAMPLE_JWT_VC_NO_NBF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtToVerifiableCredentialTransformerTest {
    private final Monitor monitor = mock();
    private final JwtToVerifiableCredentialTransformer transformer = new JwtToVerifiableCredentialTransformer(monitor);
    private final TransformerContext context = mock();

    @Test
    void transform_success() {
        var vc = transformer.transform(EXAMPLE_JWT_VC, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getType()).doesNotContainNull().isNotEmpty();
        assertThat(vc.getCredentialStatus()).isNotNull();
        assertThat(vc.getCredentialSubject()).doesNotContainNull().isNotEmpty();
        assertThat(vc.getCredentialSubject().stream().findFirst().orElseThrow().getId()).isNotNull();
        assertThat(vc.getIssuanceDate()).isNotNull();
        assertThat(vc.getContext()).contains("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1");
        assertThat(vc.getCredentialSchema()).isNotNull()
                .hasSize(2)
                .anyMatch(schema -> schema.type().equals("JsonSchema") &&
                        schema.id().equals("https://example.org/examples/degree.json"))
                .anyMatch(schema -> schema.type().equals("JsonSchema") &&
                        schema.id().equals("https://example.org/examples/alumni.json"));

        verifyNoInteractions(context);
    }

    @Test
    @DisplayName("VC claims do not contain dates, but JWT 'nbf' and 'exp' are used as fallbacks")
    void transform_credentialHasNoDates() {
        var vc = transformer.transform(EXAMPLE_JWT_VC_NO_DATES, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getIssuanceDate()).isNotNull();
        assertThat(vc.getExpirationDate()).isNotNull();
        assertThat(vc.getIssuanceDate()).isBefore(vc.getExpirationDate());

        verifyNoInteractions(context);
    }

    @Test
    void transform_jwtHasNoNbf_expectError() {
        assertThatThrownBy(() -> transformer.transform(EXAMPLE_JWT_VC_NO_NBF, context))
                .hasMessageContaining("Credential must contain `issuanceDate`/`validFrom` property.");

    }

    @Test
    void transform_vcdm20_success() {
        var vc = transformer.transform(TestData.EXAMPLE_JWT_VC_2_0, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getContext()).contains("https://www.w3.org/ns/credentials/v2", "https://www.w3.org/ns/credentials/examples/v2");
        assertThat(vc.getType()).containsExactlyInAnyOrder("VerifiableCredential", "ExampleDegreeCredential", "ExamplePersonCredential");
        assertThat(vc.getIssuer().id()).isEqualTo("https://university.example/issuers/14");
        assertThat(vc.getValidFrom().toString()).isEqualTo("2010-01-01T19:23:24Z");
    }

    @Test
    @DisplayName("VC has name and description in payload, both should be used")
    void transform_withNameAndDescription_usesVcValues() {
        var jwt = createJwt(Map.of("name", "My Credential Name", "description", "My Credential Description"));

        var vc = transformer.transform(jwt, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getName()).isEqualTo("My Credential Name");
        assertThat(vc.getDescription()).isEqualTo("My Credential Description");
        verifyNoInteractions(context);
    }

    @Test
    @DisplayName("VC has no name property, should be null")
    void transform_withoutNameInVc_fallsBackToJwtSubject() {
        var vc = transformer.transform(EXAMPLE_JWT_VC, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getName()).isNull();
        verifyNoInteractions(context);
    }

    @Test
    @DisplayName("VC has no description property, description should be null")
    void transform_withoutDescriptionInVc_descriptionIsNull() {
        var vc = transformer.transform(EXAMPLE_JWT_VC, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getDescription()).isNull();
        verifyNoInteractions(context);
    }

    private String createJwt(Map<String, Object> additionalVcProps) {
        try {
            var vcClaims = new HashMap<String, Object>();
            vcClaims.put("@context", List.of("https://www.w3.org/2018/credentials/v1"));
            vcClaims.put("type", List.of("VerifiableCredential"));
            vcClaims.put("issuanceDate", "2010-01-01T19:23:24Z");
            vcClaims.put("credentialSubject", Map.of("id", "did:example:test123", "foo", "bar"));
            vcClaims.putAll(additionalVcProps);

            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://example.edu/issuers/14")
                    .subject("did:example:test123")
                    .claim("nbf", 1262373804L)
                    .claim("vc", vcClaims)
                    .build();

            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            jwt.sign(new MACSigner("test-secret-test-secret-test-secret-12345".getBytes()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}