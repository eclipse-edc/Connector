/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponsePayloadToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultHealthCheckTest {

    private static final Result<HealthCheckResponse> HEALTH_CHECK_RESULT_200 = Result.success(HealthCheckResponse.Builder
            .newInstance()
            .payload(new HealthCheckResponsePayload())
            .code(200)
            .build());

    private static final Result<TokenLookUpResponsePayloadToken> TOKEN_LOOK_UP_RESULT_200 = Result.success(TokenLookUpResponsePayloadToken.Builder.newInstance()
            .explicitMaxTimeToLive(0)
            .isRenewable(true)
            .period(null)
            .policies(Collections.emptyList())
            .build());

    private final HashicorpVaultClient client = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultHealthCheck healthCheck = new HashicorpVaultHealthCheck(client, monitor);

    @BeforeEach
    void beforeEach() {
        reset(client);
        reset(monitor);
    }

    @Nested
    class TokenValid {

        @BeforeEach
        void beforeEach() {
            when(client.lookUpToken()).thenReturn(TOKEN_LOOK_UP_RESULT_200);
        }

        @Test
        void shouldSucceed_whenHealthCheck200() {
            when(client.doHealthCheck()).thenReturn(HEALTH_CHECK_RESULT_200);

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ValueSource(ints = {409, 472, 473, 501, 503, 999})
        void shouldFail_whenHealthCheckNot200(int code) {
            var healthCheckResponseResult = Result.success(HealthCheckResponse.Builder
                    .newInstance()
                    .payload(new HealthCheckResponsePayload())
                    .code(code)
                    .build());
            when(client.doHealthCheck()).thenReturn(healthCheckResponseResult);

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).warning(anyString());
        }

        @Test
        void shouldFail_whenHealthCheckFailed() {
            when(client.doHealthCheck()).thenReturn(Result.failure("exception message"));

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).severe("Failed to perform Healthcheck");
        }
    }

    @Nested
    class HealthCheck200 {

        @BeforeEach
        void beforeEach() {
            when(client.doHealthCheck()).thenReturn(HEALTH_CHECK_RESULT_200);
        }

        @Test
        void shouldSucceed_whenTokenLookUpSuccessful() {
            when(client.lookUpToken()).thenReturn(TOKEN_LOOK_UP_RESULT_200);

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenTokenLookUpFailed() {
            when(client.lookUpToken()).thenReturn(Result.failure("403"));

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).warning("Token look up failed: 403");
        }
    }

    @Test
    void shouldFail_whenHealthCheck429AndTokenLookUpFailed() {
        var healthCheckResponseResult = Result.success(HealthCheckResponse.Builder
                .newInstance()
                .payload(new HealthCheckResponsePayload())
                .code(429)
                .build());
        when(client.doHealthCheck()).thenReturn(healthCheckResponseResult);
        when(client.lookUpToken()).thenReturn(Result.failure("403"));

        var result = healthCheck.get();

        assertThat(result).isFailed();
        assertFailureMessagesSize(result, 2);
        verify(monitor).warning("Healthcheck unsuccessful: Vault is in standby %s".formatted(healthCheckResponseResult.getContent().getPayload()));
        verify(monitor).warning("Token look up failed: 403");
    }

    private void assertFailureMessagesSize(HealthCheckResult result, int i) {
        FailureAssert.assertThat(result.getFailure()).messages().hasSize(i);
    }
}
