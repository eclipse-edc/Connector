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

package org.eclipse.edc.token.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenValidationRuleTest {

    @Nested
    class AllOf {

        private final ClaimToken token = ClaimToken.Builder.newInstance().build();
        private final Map<String, Object> additional = Map.of("key", "value");

        @Test
        void shouldSucceed_whenNoRules() {
            var result = TokenValidationRule.allOf(List.of()).checkRule(token, additional);

            assertThat(result.succeeded()).isTrue();
        }

        @Test
        void shouldSucceed_whenAllRulesSucceed() {
            var result = TokenValidationRule.allOf(List.of(succeeding(), succeeding())).checkRule(token, additional);

            assertThat(result.succeeded()).isTrue();
        }

        @Test
        void shouldCheckEveryRule_andCollectAllFailures() {
            var last = succeeding();

            var result = TokenValidationRule.allOf(List.of(failing("failure-1"), succeeding(), failing("failure-2"), last))
                    .checkRule(token, additional);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).containsExactly("failure-1", "failure-2");
            verify(last).checkRule(token, additional);
        }

        private TokenValidationRule succeeding() {
            TokenValidationRule rule = mock();
            when(rule.checkRule(any(), any())).thenReturn(Result.success());
            return rule;
        }

        private TokenValidationRule failing(String message) {
            TokenValidationRule rule = mock();
            when(rule.checkRule(any(), any())).thenReturn(Result.failure(message));
            return rule;
        }
    }
}
