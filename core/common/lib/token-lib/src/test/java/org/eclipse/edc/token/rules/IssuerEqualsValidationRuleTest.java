/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuerEqualsValidationRuleTest {

    @Test
    void checkRule_successWhenIssuerMatches() {
        var token = createToken("issuer-a");

        var rule = new IssuerEqualsValidationRule("issuer-a");
        var result = rule.checkRule(token, null);

        assertTrue(result.succeeded());
    }

    @Test
    void checkRule_failureWhenIssuerDiffers() {
        var token = createToken("issuer-b");

        var rule = new IssuerEqualsValidationRule("issuer-a");
        var result = rule.checkRule(token, null);

        assertFalse(result.succeeded());
        // failure message contains mismatch details
        assertTrue(result.getFailureMessages().stream().anyMatch(m -> m.contains("does not match expected issuer")));
    }

    @Test
    void checkRule_failureWhenIssuerNullButExpectedNotNull() {
        var token = createToken(null);

        var rule = new IssuerEqualsValidationRule("issuer-a");
        var result = rule.checkRule(token, null);

        assertFalse(result.succeeded());
    }

    @Test
    void checkRule_successWhenBothNull() {
        var token = createToken(null);

        var rule = new IssuerEqualsValidationRule(null);
        var result = rule.checkRule(token, null);

        assertTrue(result.succeeded());
    }

    private ClaimToken createToken(String issuer) {
        return ClaimToken.Builder.newInstance().claim("iss", issuer).build();
    }
}
