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

class AccessTokenNotNullRuleTest {

    private final AccessTokenNotNullRule rule = new AccessTokenNotNullRule();

    @Test
    void accessTokenClaimPresent() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("access_token", "foobartoken").build(), Map.of()))
                .isSucceeded();
    }

    @Test
    void accessTokenClaimEmpty() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("access_token", "").build(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("The 'access_token' claim is mandatory and must not be null.");
    }

    @Test
    void accessTokenClaimNotPresent() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("The 'access_token' claim is mandatory and must not be null.");
    }
}