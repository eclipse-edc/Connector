/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class IssuerKeyIdValidationRuleTest {

    private static final String ISSUER = "did:web:issuer.example.com";
    private final IssuerKeyIdValidationRule rule = new IssuerKeyIdValidationRule();

    @Test
    void kidBelongsToIss() {
        assertThat(rule.checkRule(token(ISSUER, ISSUER + "#key-1"), Map.of()))
                .isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "did:web:attacker.example.com#key-1",
            "did:web:issuer-example.com#key-1",
            "did:web:issuer.example.com.attacker.com#key-1",
            "did:web:issuer.example.com",
            "did:web:issuer.example.com:path#key-1"
    })
    void kidDoesNotBelongToIss(String keyId) {
        assertThat(rule.checkRule(token(ISSUER, keyId), Map.of()))
                .isFailed()
                .detail().isEqualTo("kid header '%s' expected to correlate to 'iss' claim ('%s'), but it did not.".formatted(keyId, ISSUER));
    }

    @Test
    void noKid() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("iss", ISSUER).build(), Map.of()))
                .isFailed()
                .detail().isEqualTo("kid header 'null' expected to correlate to 'iss' claim ('%s'), but it did not.".formatted(ISSUER));
    }

    @Test
    void noIss() {
        var token = ClaimToken.Builder.newInstance().header("kid", "null#key-1").build();

        assertThat(rule.checkRule(token, Map.of()))
                .isFailed();
    }

    private ClaimToken token(String iss, String keyId) {
        return ClaimToken.Builder.newInstance().claim("iss", iss).header("kid", keyId).build();
    }
}
