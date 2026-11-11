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

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class PresentationTypeRuleTest {

    private final PresentationTypeRule rule = new PresentationTypeRule("VerifiablePresentation");

    @Test
    void typeString_matches() {
        assertThat(rule.checkRule(token(Map.of("type", "VerifiablePresentation")), Map.of())).isSucceeded();
    }

    @Test
    void typeList_matches() {
        assertThat(rule.checkRule(token(Map.of("type", List.of("VerifiablePresentation", "ExamplePresentation"))), Map.of())).isSucceeded();
    }

    @Test
    void noType_fails() {
        assertThat(rule.checkRule(token(Map.of()), Map.of())).isFailed()
                .detail().contains("missing the 'type' claim");
    }

    @Test
    void otherType_fails() {
        assertThat(rule.checkRule(token(Map.of("type", List.of("EnvelopedVerifiablePresentation"))), Map.of())).isFailed()
                .detail().contains("expected type 'VerifiablePresentation'");
    }

    private ClaimToken token(Map<String, Object> claims) {
        return ClaimToken.Builder.newInstance().claims(claims).build();
    }
}
