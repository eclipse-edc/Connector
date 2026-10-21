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

    @Test
    void kidBelongsToIss() {
        assertThat(new IssuerKeyIdValidationRule(ISSUER + "#key-1").checkRule(token(ISSUER), Map.of()))
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
        assertThat(new IssuerKeyIdValidationRule(keyId).checkRule(token(ISSUER), Map.of()))
                .isFailed()
                .detail().isEqualTo("kid header '%s' expected to correlate to 'iss' claim ('%s'), but it did not.".formatted(keyId, ISSUER));
    }

    @Test
    void noKid() {
        assertThat(new IssuerKeyIdValidationRule(null).checkRule(token(ISSUER), Map.of()))
                .isFailed();
    }

    @Test
    void noIss() {
        assertThat(new IssuerKeyIdValidationRule("null#key-1").checkRule(ClaimToken.Builder.newInstance().build(), Map.of()))
                .isFailed();
    }

    private ClaimToken token(String iss) {
        return ClaimToken.Builder.newInstance().claim("iss", iss).build();
    }
}
