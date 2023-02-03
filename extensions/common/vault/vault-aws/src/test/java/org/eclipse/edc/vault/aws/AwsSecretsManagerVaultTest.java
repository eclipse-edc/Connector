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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AwsSecretsManagerVaultTest {

    private final Monitor monitor = mock(Monitor.class);
    private final SecretsManagerClient secretClient = mock(SecretsManagerClient.class);
    private final AwsSecretsManagerVaultSanitationStrategy sanitizer =
            new AwsSecretsManagerVaultDefaultSanitationStrategy(monitor);
    private final AwsSecretsManagerVault vault = new AwsSecretsManagerVault(secretClient, monitor,
            sanitizer);

    @Test
    void storeSecret_shouldSanitizeKey() {
        var key = "invalid#key";
        var value = "value";

        vault.storeSecret(key, value);

        verify(secretClient).createSecret(CreateSecretRequest.builder().name(sanitizer.sanitizeKey(key))
                .secretString(value).build());
    }

    @Test
    void storeSecret_shouldNotOverwriteSecrets() {
        var key = "valid-key";
        var value = "value";

        vault.storeSecret(key, value);

        verify(secretClient).createSecret(CreateSecretRequest.builder().name(sanitizer.sanitizeKey(key))
                .secretString(value).build());
    }

    @Test
    void resolveSecret_shouldSanitizeKey() {
        var key = "valid-key";

        vault.resolveSecret(key);

        verify(secretClient).getSecretValue(GetSecretValueRequest.builder().secretId(sanitizer.sanitizeKey(key))
                .build());
    }

    @Test
    void deleteSecret_shouldSanitizeKey() {
        var key = "valid-key";

        vault.deleteSecret(key);

        verify(secretClient).deleteSecret(DeleteSecretRequest.builder().secretId(sanitizer.sanitizeKey(key))
                .forceDeleteWithoutRecovery(true)
                .build());
    }

    @Test
    void resolveSecret_shouldNotLogSevereIfSecretNotFound() {
        when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(sanitizer.sanitizeKey("key"))
                .build()))
                .thenThrow(ResourceNotFoundException.builder().build());

        var result = vault.resolveSecret("key");

        assertThat(result).isNull();
        verify(monitor, times(2))
                .debug(ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void resolveSecret_shouldReturnNullAndLogErrorOnGenericException() {
        when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(sanitizer.sanitizeKey("key"))
                        .build()))
                .thenThrow(new RuntimeException("test"));

        var result = vault.resolveSecret("key");

        assertThat(result).isNull();
        verify(monitor).debug(ArgumentMatchers.anyString());
        verify(monitor).severe(ArgumentMatchers.anyString(), ArgumentMatchers.isA(RuntimeException.class));
    }
}