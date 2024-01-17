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
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;
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
