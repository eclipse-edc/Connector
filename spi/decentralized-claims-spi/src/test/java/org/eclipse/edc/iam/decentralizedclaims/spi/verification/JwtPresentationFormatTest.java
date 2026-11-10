/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;

class JwtPresentationFormatTest {

    private static final String VC_V1_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    private static final String VC_V2_CONTEXT = "https://www.w3.org/ns/credentials/v2";

    @Test
    void vcdm11Presentation() {
        assertThat(JwtPresentationFormat.of(Map.of("vp", Map.of()))).isEqualTo(VC1_0_JWT);
        assertThat(JwtPresentationFormat.of(Map.of("vp", Map.of(), "@context", List.of(VC_V1_CONTEXT)))).isEqualTo(VC1_0_JWT);
    }

    @Test
    void vcdm20Presentation() {
        assertThat(JwtPresentationFormat.of(Map.of("@context", List.of(VC_V2_CONTEXT, "https://example.com/context")))).isEqualTo(VC2_0_JOSE);
        assertThat(JwtPresentationFormat.of(Map.of("@context", VC_V2_CONTEXT))).isEqualTo(VC2_0_JOSE);
    }

    @Test
    void bothFormats_isNoPresentation() {
        assertThat(JwtPresentationFormat.of(Map.of("vp", Map.of(), "@context", List.of(VC_V2_CONTEXT)))).isNull();
        assertThat(JwtPresentationFormat.of(Map.of("vp", Map.of(), "@context", VC_V2_CONTEXT))).isNull();
    }

    @Test
    void vcClaim_isNoPresentation() {
        assertThat(JwtPresentationFormat.of(Map.of("vc", Map.of()))).isNull();
        assertThat(JwtPresentationFormat.of(Map.of("vp", Map.of(), "vc", Map.of()))).isNull();
        assertThat(JwtPresentationFormat.of(Map.of("vc", Map.of(), "@context", List.of(VC_V2_CONTEXT)))).isNull();
    }

    @Test
    void neitherFormat_isNoPresentation() {
        assertThat(JwtPresentationFormat.of(Map.of())).isNull();
        assertThat(JwtPresentationFormat.of(Map.of("@context", List.of(VC_V1_CONTEXT)))).isNull();
    }
}
