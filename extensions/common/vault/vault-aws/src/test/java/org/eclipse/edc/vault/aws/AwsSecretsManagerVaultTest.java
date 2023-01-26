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
import org.mockito.Mockito;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

class AwsSecretsManagerVaultTest {

    private final Monitor monitor = Mockito.mock(Monitor.class);
    private final SecretsManagerClient secretClient = Mockito.mock(SecretsManagerClient.class);

    private final AwsSecretsManagerVault vault = new AwsSecretsManagerVault(secretClient, monitor);

    @Test
    void resolveSecret_sanitizeKeyNameReplacesInvalidCharacters() {
        for (var validCharacter : List.of('_', '+', '-', '@', '/', '.')) {
            var validKey = "valid" + validCharacter + "key";
            assertThat(vault.sanitizeKey(validKey)).isEqualTo(validKey + '_' + validKey.hashCode());
        }
        var key2 = "invalid#key";
        var sanitized = vault.sanitizeKey(key2);
        assertThat(sanitized).isEqualTo("invalid-key" + "_" + key2.hashCode());
    }

    @Test
    void resolveSecret_sanitizeKeyNameLimitsKeySize() {
        var key = "-".repeat(10000);
        var sanitized = vault.sanitizeKey(key);
        assertThat(sanitized)
                .isEqualTo("-".repeat(500) + "_" + key.hashCode());
        assertThat(sanitized.length()).isEqualTo(512);
    }

    @Test
    void resolveSecret_sanitizeKeyNameLimitsKeySize2() {
        var key = "-".repeat(500);
        var sanitized = vault.sanitizeKey(key);
        assertThat(sanitized)
                .isEqualTo("-".repeat(500) + "_" + key.hashCode());
        assertThat(sanitized.length()).isLessThanOrEqualTo(512);
    }


    @Test
    void storeSecret_shouldSanitizeKey() {
        var key = "invalid#key";
        var value = "value";
        vault.storeSecret(key, value);
        Mockito.verify(secretClient).createSecret(CreateSecretRequest.builder().name(vault.sanitizeKey(key))
                .secretString(value).build());
    }

    @Test
    void storeSecret_shouldNotOverwriteSecrets() {
        var key = "valid-key";
        var value = "value";
        vault.storeSecret(key, value);
        Mockito.verify(secretClient).createSecret(CreateSecretRequest.builder().name(vault.sanitizeKey(key))
                .secretString(value).build());
    }

    @Test
    void resolveSecret_shouldSanitizeKey() {
        var key = "valid-key";
        vault.resolveSecret(key);
        Mockito.verify(secretClient).getSecretValue(GetSecretValueRequest.builder().secretId(vault.sanitizeKey(key))
                .build());
    }

    @Test
    void deleteSecret_shouldSanitizeKey() {
        var key = "valid-key";
        vault.deleteSecret(key);
        Mockito.verify(secretClient).deleteSecret(DeleteSecretRequest.builder().secretId(vault.sanitizeKey(key))
                .forceDeleteWithoutRecovery(true)
                .build());
    }

    @Test
    void resolveSecret_shouldNotLogSevereIfSecretNotFound() {
        Mockito.when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(vault.sanitizeKey("key"))
                .build()))
                .thenThrow(ResourceNotFoundException.builder().build());

        var result = vault.resolveSecret("key");

        assertThat(result).isNull();
        Mockito.verify(monitor, times(2))
                .debug(ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void resolveSecret_shouldReturnNullAndLogErrorOnGenericException() {
        Mockito.when(secretClient.getSecretValue(GetSecretValueRequest.builder().secretId(vault.sanitizeKey("key"))
                        .build()))
                .thenThrow(new RuntimeException("test"));
        var result = vault.resolveSecret("key");
        assertThat(result).isNull();
        Mockito.verify(monitor).debug(ArgumentMatchers.anyString());
        Mockito.verify(monitor).severe(ArgumentMatchers.anyString(), ArgumentMatchers.isA(RuntimeException.class));
    }
}