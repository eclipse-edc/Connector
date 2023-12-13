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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponsePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultHealthCheckTest {

    private HashicorpVaultHealthCheck healthCheck;

    private final Monitor monitor = mock();
    private final HashicorpVaultClient client = mock();

    @BeforeEach
    void setup() {
        healthCheck = new HashicorpVaultHealthCheck(client, monitor);
    }

    @Test
    void shouldSucceed_whenClientReturns200() {
        var response = HealthCheckResponse.Builder.newInstance().payload(new HealthCheckResponsePayload()).code(200).build();
        when(client.doHealthCheck()).thenReturn(response);

        var result = healthCheck.get();

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(ints = {409, 472, 473, 501, 503, 999})
    void shouldFail_whenClientReturnsErrorCodes(int code) {
        var response = HealthCheckResponse.Builder.newInstance().payload(new HealthCheckResponsePayload()).code(code).build();
        when(client.doHealthCheck()).thenReturn(response);

        var result = healthCheck.get();

        assertThat(result).isFailed();
        verify(monitor, times(1)).warning(anyString());
    }

    @Test
    void testResponseFromException() {
        when(client.doHealthCheck()).thenThrow(new EdcException("foo-bar"));

        var result = healthCheck.get();

        assertThat(result).isFailed();
    }
}
