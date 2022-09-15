/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.azure;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.HttpResponse;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureVaultTest {

    private final Monitor monitor = mock(Monitor.class);
    private final SecretClient secretClient = mock(SecretClient.class);

    private final AzureVault vault = new AzureVault(monitor, secretClient);

    @Test
    void resolveSecret() {
        when(secretClient.getSecret("key")).thenReturn(new KeyVaultSecret("key", "secret"));

        var result = vault.resolveSecret("key");

        assertThat(result).isEqualTo("secret");
    }

    @Test
    void resolveSecret_sanitizeKeyName() {
        when(secretClient.getSecret("key-name")).thenReturn(new KeyVaultSecret("key-name", "secret"));

        var result = vault.resolveSecret("key.name");

        assertThat(result).isEqualTo("secret");
        verify(secretClient).getSecret("key-name");
    }

    @Test
    void resolveSecret_shouldNotLogSevereIfSecretNotFound() {
        when(secretClient.getSecret("key")).thenThrow(new ResourceNotFoundException("error", mock(HttpResponse.class)));

        var result = vault.resolveSecret("key");

        assertThat(result).isNull();
        verify(monitor).debug(anyString());
    }

    @Test
    void resolveSecret_shouldReturnNullAndLogErrorOnGenericException() {
        when(secretClient.getSecret("key")).thenThrow(new RuntimeException("error"));

        var result = vault.resolveSecret("key");

        assertThat(result).isNull();
        verify(monitor).severe(anyString(), isA(RuntimeException.class));
    }
}