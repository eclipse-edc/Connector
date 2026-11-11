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

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class PresentationFormatRuleTest {

    private static final List<String> V2_CONTEXT = List.of("https://www.w3.org/ns/credentials/v2");

    @Test
    void vcdm11_matches() {
        assertThat(new PresentationFormatRule(CredentialFormat.VC1_0_JWT).checkRule(token(Map.of("vp", Map.of())), Map.of()))
                .isSucceeded();
    }

    @Test
    void vcdm11_vcdm20Context_fails() {
        assertThat(new PresentationFormatRule(CredentialFormat.VC1_0_JWT).checkRule(token(Map.of("vp", Map.of(), "@context", V2_CONTEXT)), Map.of()))
                .isFailed().detail().contains("Not a valid VCDM 1.1 presentation");
    }

    @Test
    void vcdm20_matches() {
        assertThat(new PresentationFormatRule(CredentialFormat.VC2_0_JOSE).checkRule(token(Map.of("@context", V2_CONTEXT)), Map.of()))
                .isSucceeded();
    }

    @Test
    void vcdm20_vpClaim_fails() {
        assertThat(new PresentationFormatRule(CredentialFormat.VC2_0_JOSE).checkRule(token(Map.of("vp", Map.of(), "@context", V2_CONTEXT)), Map.of()))
                .isFailed().detail().contains("Not a valid VCDM 2.0 presentation");
    }

    @Test
    void vcClaim_fails() {
        assertThat(new PresentationFormatRule(CredentialFormat.VC2_0_JOSE).checkRule(token(Map.of("vc", Map.of(), "@context", V2_CONTEXT)), Map.of()))
                .isFailed();
    }

    @Test
    void unsupportedFormat_throws() {
        assertThatThrownBy(() -> new PresentationFormatRule(CredentialFormat.VC1_0_LD)).isInstanceOf(IllegalArgumentException.class);
    }

    private ClaimToken token(Map<String, Object> claims) {
        return ClaimToken.Builder.newInstance().claims(claims).build();
    }
}
