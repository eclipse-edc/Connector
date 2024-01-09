/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.verifiablecredentials.jwt.rules;


import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

/**
 * Validates that both the "iss" and "aud" claims are non-null and are both equal.
 * This is required for self-issued ID tokens.
 */
class IssuerEqualsSubjectRuleTest {

    private final IssuerEqualsSubjectRule rule = new IssuerEqualsSubjectRule();

    @Test
    void issSubEquals() {
        var ct = createToken("test-iss", "test-iss");
        assertThat(rule.checkRule(ct, Map.of())).isSucceeded();
    }

    @Test
    void issSubNotEquals() {
        var ct = createToken("test-iss", "test-sub");
        assertThat(rule.checkRule(ct, Map.of())).isFailed()
                .detail()
                .isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @Test
    void issSubEqualsBothNull() {
        var ct = ClaimToken.Builder.newInstance().build();
        assertThat(rule.checkRule(ct, Map.of())).isFailed()
                .detail()
                .isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @Test
    void issSub_issIsNull() {
        var ct = ClaimToken.Builder.newInstance().claim("sub", "test-sub").build();
        assertThat(rule.checkRule(ct, Map.of())).isFailed()
                .detail()
                .isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    private ClaimToken createToken(String iss, String sub) {
        return ClaimToken.Builder.newInstance().claims(Map.of("iss", iss, "sub", sub)).build();
    }
}