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

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class NotPresentationRuleTest {

    private final NotPresentationRule rule = new NotPresentationRule();

    @Test
    void credential_succeeds() {
        assertThat(rule.checkRule(token(Map.of("type", List.of("VerifiableCredential"))), Map.of())).isSucceeded();
    }

    @Test
    void noType_succeeds() {
        assertThat(rule.checkRule(token(Map.of("vc", Map.of())), Map.of())).isSucceeded();
    }

    @Test
    void vpClaim_fails() {
        assertThat(rule.checkRule(token(Map.of("vp", Map.of())), Map.of())).isFailed()
                .detail().contains("Expected a credential, but the JWT contains a presentation.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "VerifiablePresentation", "EnvelopedVerifiablePresentation" })
    void presentationType_fails(String type) {
        assertThat(rule.checkRule(token(Map.of("type", type)), Map.of())).isFailed();
        assertThat(rule.checkRule(token(Map.of("type", List.of(type))), Map.of())).isFailed();
    }

    private ClaimToken token(Map<String, Object> claims) {
        return ClaimToken.Builder.newInstance().claims(claims).build();
    }
}
