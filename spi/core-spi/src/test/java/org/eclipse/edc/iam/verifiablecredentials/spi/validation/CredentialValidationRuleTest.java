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

package org.eclipse.edc.iam.verifiablecredentials.spi.validation;

import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialValidationRuleTest {

    @Test
    void and_shouldSucceed_whenBothRulesSucceed() {
        CredentialValidationRule rule = credential -> Result.success();

        assertThat(rule.and(credential -> Result.success()).apply(null).succeeded()).isTrue();
    }

    @Test
    void and_shouldReportTheFailuresOfBothRules() {
        CredentialValidationRule first = credential -> Result.failure("first");
        CredentialValidationRule second = credential -> Result.failure("second");

        var result = first.and(second).apply(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("first", "second");
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    void and_shouldBeRetryable_whenEveryFailedRuleIsRetryable() {
        CredentialValidationRule first = credential -> Result.retryableFailure("status list not reachable");
        CredentialValidationRule second = credential -> Result.success();

        assertThat(first.and(second).apply(null).isRetryable()).isTrue();
        assertThat(second.and(first).apply(null).isRetryable()).isTrue();
        assertThat(first.and(first).apply(null).isRetryable()).isTrue();
    }

    @Test
    void and_shouldNotBeRetryable_whenAnyFailedRuleIsNotRetryable() {
        CredentialValidationRule retryable = credential -> Result.retryableFailure("status list not reachable");
        CredentialValidationRule plain = credential -> Result.failure("credential expired");

        assertThat(retryable.and(plain).apply(null).isRetryable()).isFalse();
        assertThat(plain.and(retryable).apply(null).isRetryable()).isFalse();
    }
}
