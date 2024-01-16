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
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultHealthCheckTest {

    private static final Result<TokenLookUpResponse> TOKEN_LOOK_UP_RESULT_200 = Result.success(TokenLookUpResponse.Builder.newInstance().build());

    private final HashicorpVaultClient client = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultHealthCheck healthCheck = new HashicorpVaultHealthCheck(client, monitor);

    @Nested
    class TokenValid {

        @BeforeEach
        void beforeEach() {
            when(client.lookUpToken(0L)).thenReturn(TOKEN_LOOK_UP_RESULT_200);
        }

        @Test
        void get_whenHealthCheckSucceeded_shouldSucceed() {
            when(client.doHealthCheck()).thenReturn(Result.success());

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @Test
        void get_whenHealthCheckFailed_shouldFail() {
            var healthCheckErr = "Vault is not available. Reason: Vault is in standby, additional information: hello";
            when(client.doHealthCheck()).thenReturn(Result.failure(healthCheckErr));

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).warning("Healthcheck failed with reason(s): %s".formatted(healthCheckErr));
        }
    }

    @Nested
    class HealthCheck200 {

        @BeforeEach
        void beforeEach() {
            when(client.doHealthCheck()).thenReturn(Result.success());
        }

        @Test
        void get_whenTokenValid_shouldSucceed() {
            when(client.lookUpToken(0L)).thenReturn(TOKEN_LOOK_UP_RESULT_200);

            var result = healthCheck.get();

            assertThat(result).isSucceeded();
        }

        @Test
        void get_whenTokenNotValid_shouldFail() {
            var tokenLookUpErr = "Token look up failed with status 403";
            when(client.lookUpToken(0L)).thenReturn(Result.failure(tokenLookUpErr));

            var result = healthCheck.get();

            assertThat(result).isFailed();
            assertFailureMessagesSize(result, 1);
            verify(monitor).warning("Healthcheck failed with reason(s): %s".formatted(tokenLookUpErr));
        }
    }

    @Test
    void get_whenHealthCheckFailedAndTokenNotValid_shouldFail() {
        var healthCheckErr = "Vault is not available. Reason: Vault is in standby, additional information: hello";
        var tokenLookUpErr = "Token look up failed with status 403";

        when(client.doHealthCheck()).thenReturn(Result.failure(healthCheckErr));
        when(client.lookUpToken(0L)).thenReturn(Result.failure(tokenLookUpErr));

        var result = healthCheck.get();

        assertThat(result).isFailed();
        assertFailureMessagesSize(result, 2);
        verify(monitor).warning("Healthcheck failed with reason(s): %s, %s".formatted(healthCheckErr, tokenLookUpErr));
    }

    private void assertFailureMessagesSize(HealthCheckResult result, int i) {
        FailureAssert.assertThat(result.getFailure()).messages().hasSize(i);
    }
}
