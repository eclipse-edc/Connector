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

import org.eclipse.edc.iam.decentralizedclaims.transform.TestData;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertThat(vc.getType()).containsExactlyInAnyOrder("VerifiableCredential", "ExampleDegreeCredential", "ExamplePersonCredential");
        assertThat(vc.getIssuer().id()).isEqualTo("https://university.example/issuers/14");
        assertThat(vc.getValidFrom().toString()).isEqualTo("2010-01-01T19:23:24Z");
    }
}