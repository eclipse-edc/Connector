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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEW_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {

    private static final String URL = "https://test.com/vault";
    private static final String TOKEN = "some-token";

    private HashicorpVaultExtension extension;
    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final EdcHttpClient httpClient = mock();

    @BeforeEach
    void beforeEach(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        when(context.getSetting(VAULT_URL, null)).thenReturn(URL);
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn(TOKEN);
        extension = factory.constructInstance(HashicorpVaultExtension.class);
    }

    @Test
    void hashicorpVault_ensureType(ServiceExtensionContext context) {
        extension.initialize(context);
        assertThat(extension.hashicorpVault()).isInstanceOf(HashicorpVault.class);
    }

    @Test
    void start_withTokenRenewEnabled_shouldStartTokenRenewTask(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultTokenRenewTask.class, (client, mockContext) -> {})) {
            extension.initialize(context);
            extension.start();
            var renewTask = mockedConstruction.constructed().get(0);
            verify(renewTask).start();
        }
    }

    @Test
    void start_withTokenRenewDisabled_shouldNotStartTokenRenewTask(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultTokenRenewTask.class, (client, mockContext) -> {})) {
            when(context.getSetting(VAULT_TOKEN_SCHEDULED_RENEW_ENABLED, VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT)).thenReturn(false);
            extension.initialize(context);
            extension.start();
            var renewTask = mockedConstruction.constructed().get(0);
            verify(renewTask, never()).start();
        }
    }

    @Test
    void shutdown_withTokenRenewTaskRunning_shouldStopTokenRenewTask(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultTokenRenewTask.class, (client, mockContext) -> {})) {
            extension.initialize(context);
            var renewTask = mockedConstruction.constructed().get(0);
            when(renewTask.isRunning()).thenReturn(true);
            extension.shutdown();
            verify(renewTask).stop();
        }
    }

    @Test
    void shutdown_withTokenRenewTaskNotRunning_shouldNotStopTokenRenewTask(ServiceExtensionContext context) {
        try (var mockedConstruction = mockConstruction(HashicorpVaultTokenRenewTask.class, (client, mockContext) -> {})) {
            extension.initialize(context);
            var renewTask = mockedConstruction.constructed().get(0);
            when(renewTask.isRunning()).thenReturn(false);
            extension.shutdown();
            verify(renewTask, never()).stop();
        }
    }
}
