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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {

    private HashicorpVaultExtension extension;
    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final EdcHttpClient httpClient = mock();

    @BeforeEach
    void beforeEach(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        extension = factory.constructInstance(HashicorpVaultExtension.class);
        when(context.getSetting(VAULT_URL, null)).thenReturn("foo");
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn("foo");
    }

    @Test
    void hashicorpVaultClient_whenVaultUrlUndefined_shouldThrowEdcException(ServiceExtensionContext context) {
        when(context.getSetting(VAULT_URL, null)).thenReturn(null);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault URL must not be null");
    }

    @Test
    void hashicorpVaultClient_whenVaultTokenUndefined_shouldThrowEdcException(ServiceExtensionContext context) {
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn(null);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault token must not be null");
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0.9})
    void hashicorpVaultClient_whenVaultRetryBackoffBaseNotGreaterThan1_shouldThrowEdcException(double value, ServiceExtensionContext context) {
        when(context.getSetting(VAULT_RETRY_BACKOFF_BASE, VAULT_RETRY_BACKOFF_BASE_DEFAULT)).thenReturn(value);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault retry exponential backoff base be greater than 1");
    }

    @Test
    void hashicorpVaultClient_whenVaultTokenTtlLessThanZero_shouldThrowEdcException(ServiceExtensionContext context) {
        when(context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_DEFAULT)).thenReturn(-1L);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault token ttl must not be negative");
    }

    @Test
    void hashicorpVaultClient_whenVaultTokenRenewBufferLessThanZero_shouldThrowEdcException(ServiceExtensionContext context) {
        when(context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_DEFAULT)).thenReturn(-1L);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault token renew buffer must not be negative");
    }

    @Test
    void hashicorpVaultClient_whenVaultTokenRenewBufferGreaterThanTtl_shouldThrowEdcException(ServiceExtensionContext context) {
        when(context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_DEFAULT)).thenReturn(10L);
        when(context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_DEFAULT)).thenReturn(30L);

        var throwable = assertThrows(EdcException.class, () -> extension.hashicorpVaultClient(context));
        assertThat(throwable.getMessage()).isEqualTo("[Hashicorp Vault Extension] Vault token ttl must be greater than renew buffer");
    }

    @Test
    void hashicorpVault_whenVaultUrlUndefined_expectException(ServiceExtensionContext context, HashicorpVaultExtension extension) {
        when(context.getSetting(VAULT_URL, null)).thenReturn(null);

        assertThatThrownBy(() -> extension.hashicorpVault(context)).isInstanceOf(EdcException.class);
    }

    @Test
    void hashicorpVault_whenVaultTokenUndefined_expectException(ServiceExtensionContext context, HashicorpVaultExtension extension) {
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn(null);
        when(context.getSetting(VAULT_URL, null)).thenReturn("https://some.vault");

        assertThrows(EdcException.class, () -> extension.hashicorpVault(context));
    }

    @Test
    void hashicorpVault_ensureType(HashicorpVaultExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(VAULT_URL, null)).thenReturn("https://some.vault");
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn("some-token");

        assertThat(extension.hashicorpVault(context)).isInstanceOf(HashicorpVault.class);
    }

    @Test
    void start_withScheduledTokenRenewalEnabled_shouldScheduleTokenRenewal(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultClient.class, (client, mockContext) -> {})) {
            extension.hashicorpVaultClient(context);
            extension.initialize(context);
            extension.start();
            var hashicorpVaultClient = mockedConstruction.constructed().get(0);
            verify(hashicorpVaultClient).scheduleTokenRenewal();
        }
    }

    @Test
    void start_withScheduledTokenRenewalDisabled_shouldNotScheduleTokenRenewal(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultClient.class, (client, mockContext) -> {})) {
            when(context.getSetting(VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED, VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT)).thenReturn(false);
            extension.hashicorpVaultClient(context);
            extension.initialize(context);
            extension.start();
            var hashicorpVaultClient = mockedConstruction.constructed().get(0);
            verify(hashicorpVaultClient, never()).scheduleTokenRenewal();
        }
    }

    @Test
    void shutdown_shouldShutdownScheduledExecutorService(ServiceExtensionContext context) {
        var scheduledExecutorService = mock(ScheduledExecutorService.class);
        when(executorInstrumentation.instrument(any(ScheduledExecutorService.class), eq(extension.name()))).thenReturn(scheduledExecutorService);
        extension.initialize(context);
        extension.shutdown();
        verify(scheduledExecutorService).shutdownNow();
    }
}
