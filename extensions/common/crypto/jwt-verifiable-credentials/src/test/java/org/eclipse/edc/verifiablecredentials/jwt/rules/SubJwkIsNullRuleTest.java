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

class SubJwkIsNullRuleTest {

    private final SubJwkIsNullRule rule = new SubJwkIsNullRule();

    @Test
    void subJwkIsNull() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of())).isSucceeded();
    }

    @Test
    void subJwkIsEmpty() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("sub_jwk", "").build(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("The 'sub_jwk' claim must not be present.");
    }

    @Test
    void subJwkIsPresent() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("sub_jwk", "foobar").build(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("The 'sub_jwk' claim must not be present.");
    }
}