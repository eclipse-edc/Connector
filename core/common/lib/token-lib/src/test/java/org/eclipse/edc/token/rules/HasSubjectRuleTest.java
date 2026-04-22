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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class HasSubjectRuleTest {

    private final HasSubjectRule rule = new HasSubjectRule();

    @Test
    void subjectClaimPresent() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().claim("sub", "subject-test").build(), Map.of());

        assertThat(result).isSucceeded();
    }

    @Test
    void subjectClaimEmpty() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().claim("sub", "").build(), Map.of());

        assertThat(result)
                .isFailed()
                .detail()
                .isEqualTo("The 'sub' claim is mandatory and must not be null.");
    }

    @Test
    void subjectClaimNotPresent() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of());

        assertThat(result)
                .isFailed()
                .detail()
                .isEqualTo("The 'sub' claim is mandatory and must not be null.");
    }
}