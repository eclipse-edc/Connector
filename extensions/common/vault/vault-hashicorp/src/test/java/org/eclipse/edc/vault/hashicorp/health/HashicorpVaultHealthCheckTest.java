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
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp.health;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultHealthService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.assertions.FailureAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultHealthCheckTest {

    private final HashicorpVaultHealthService client = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultHealthCheck healthCheck = new HashicorpVaultHealthCheck(client, monitor);

    @Nested
    class TokenValid {

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
            assertThat(result.getFailure()).messages().hasSize(1);
            verify(monitor).debug("Vault health check failed with reason(s): %s".formatted(healthCheckErr));
        }
    }

}
