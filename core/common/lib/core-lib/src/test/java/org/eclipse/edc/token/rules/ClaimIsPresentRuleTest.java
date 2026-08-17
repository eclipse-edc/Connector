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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimIsPresentRuleTest {

    private final ClaimIsPresentRule rule = new ClaimIsPresentRule("scope");

    @Test
    void shouldSucceed_whenClaimIsPresent() {
        var token = ClaimToken.Builder.newInstance().claim("scope", "signaling:dataflow").build();

        assertThat(rule.checkRule(token, null).succeeded()).isTrue();
    }

    @Test
    void shouldFail_whenClaimIsAbsent() {
        var token = ClaimToken.Builder.newInstance().claim("sub", "a-sub").build();

        var result = rule.checkRule(token, null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("scope");
    }
}
