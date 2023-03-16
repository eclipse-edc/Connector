/*
 *  Copyright (c) 2023 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - Initial Implementation
 *
 */

package org.eclipse.edc.vault.aws;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(Lifecycle.PER_CLASS)
class AwsSecretsManagerVaultTest {

    private static final String KEY = "valid-key";
    private static final String SANITIZED_KEY = "valid-key-sanitized";
    private final Monitor monitor = mock(Monitor.class);
    private final SecretsManagerClient secretClient = mock(SecretsManagerClient.class);
    private final AwsSecretsManagerVaultSanitationStrategy sanitizer = mock(AwsSecretsManagerVaultSanitationStrategy.class);
    private final AwsSecretsManagerVault vault = new AwsSecretsManagerVault(secretClient, monitor,
            sanitizer);

    @BeforeAll
    void setup() {
        when(sanitizer.sanitizeKey(KEY)).thenReturn(SANITIZED_KEY);
    }

    @BeforeEach
    void resetMocks() {
        reset(monitor, secretClient);
    }

    @Test
    void storeSecret_shouldSanitizeKey() {
        var value = "value";

        vault.storeSecret(KEY, value);

        verify(secretClient).createSecret(CreateSecretRequest.builder().name(SANITIZED_KEY)
                .secretString(value).build());
    }

    @Test
    void storeSecret_shouldNotOverwriteSecrets() {
        var value = "value";

        vault.storeSecret(KEY, value);

        verify(secretClient).createSecret(CreateSecretRequest.builder().name(SANITIZED_KEY)
                .secretString(value).build());
    }

    @Test
    void resolveSecret_shouldSanitizeKey() {
        vault.resolveSecret(KEY);

        verify(secretClient).getSecretValue(GetSecretValueRequest.builder().secretId(SANITIZED_KEY)
                .build());
    }

    @Test
    void deleteSecret_shouldSanitizeKey() {
        vault.deleteSecret(KEY);

        verify(secretClient).deleteSecret(DeleteSecretRequest.builder().secretId(SANITIZED_KEY)
                .forceDeleteWithoutRecovery(true)
                .build());
    }

    @Test
    void resolveSecret_shouldNotLogSevereIfSecretNotFound() {
        when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(SANITIZED_KEY)
                .build()))
                .thenThrow(ResourceNotFoundException.builder().build());

        var result = vault.resolveSecret(KEY);

        assertThat(result).isNull();
        verify(monitor, times(1))
                .debug(anyString());

        verify(monitor, times(1))
                .debug(anyString(), any());
    }

    @Test
    void resolveSecret_shouldReturnNullAndLogErrorOnGenericException() {
        when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(SANITIZED_KEY)
                .build()))
                .thenThrow(new RuntimeException("test"));

        var result = vault.resolveSecret(KEY);

        assertThat(result).isNull();
        verify(monitor).debug(anyString());
        verify(monitor).severe(anyString(), ArgumentMatchers.isA(RuntimeException.class));
    }
}
