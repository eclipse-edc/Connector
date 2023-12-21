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
            .isRenewable(true)
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
        void get_whenHealthCheck200_shouldSucceed() {
            when(client.doHealthCheck()).thenReturn(HEALTH_CHECK_RESULT_200);

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ValueSource(ints = {409, 472, 473, 501, 503, 999})
        void get_whenHealthCheckIsNot200_shouldFail(int code) {
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
        void get_whenHealthCheckFailed_shouldFail() {
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
        void get_whenTokenValid_shouldSucceed() {
            when(client.lookUpToken()).thenReturn(TOKEN_LOOK_UP_RESULT_200);

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @Test
        void get_whenTokenNotValid_shouldFail() {
            when(client.lookUpToken()).thenReturn(Result.failure("403"));

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).warning("Healthcheck failed with reason(s): Token look up failed: 403");
        }
    }

    @Test
    void get_whenHealthCheckNot200AndTokenNotValid_shouldFail() {
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
        verify(monitor).warning("Healthcheck failed with reason(s): Healthcheck unsuccessful: Vault is in standby " +
                "HealthResponsePayload{isInitialized=false, isSealed=false, isStandby=false, isPerformanceStandby=false, " +
                "replicationPerformanceMode='null', replicationDrMode='null', serverTimeUtc=0, version='null', clusterName='null', clusterId='null'}, " +
                "Token look up failed: 403");
    }

    private void assertFailureMessagesSize(HealthCheckResult result, int i) {
        FailureAssert.assertThat(result.getFailure()).messages().hasSize(i);
    }
}
