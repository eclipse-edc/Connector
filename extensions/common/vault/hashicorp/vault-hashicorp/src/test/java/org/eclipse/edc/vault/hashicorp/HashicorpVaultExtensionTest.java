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
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {

    private static final String URL = "https://test.com/vault";
    private static final String TOKEN = "some-token";
    private static final String VAULT_URL = "edc.vault.hashicorp.url";
    private static final String VAULT_TOKEN = "edc.vault.hashicorp.token";
    private static final String VAULT_TOKEN_SCHEDULED_RENEW_ENABLED = "edc.vault.hashicorp.token.scheduled-renew-enabled";

    private HashicorpVaultExtension extension;
    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final ScheduledExecutorService scheduledExecutorService = mock();
    private final EdcHttpClient httpClient = mock();

    @BeforeEach
    void beforeEach(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);

        var config = ConfigFactory.fromMap(Map.of(
                VAULT_URL, URL,
                VAULT_TOKEN, TOKEN
        ));
        when(context.getConfig()).thenReturn(config);
        when(executorInstrumentation.instrument(any(), anyString())).thenReturn(scheduledExecutorService);
        extension = factory.constructInstance(HashicorpVaultExtension.class);
    }

    @Test
    void hashicorpVault_ensureType(ServiceExtensionContext context) {
        extension.initialize(context);
        assertThat(extension.hashicorpVault()).isInstanceOf(HashicorpVault.class);
    }

    @Test
    void start_withTokenRenewEnabled_shouldStartTokenRenewTask(ServiceExtensionContext context) {
        extension.initialize(context);
        extension.start();
        verify(executorInstrumentation).instrument(any(), anyString());
        verify(scheduledExecutorService).execute(any());
    }

    @Test
    void start_withTokenRenewDisabled_shouldNotStartTokenRenewTask(ServiceExtensionContext context, ObjectFactory factory) {

        var config = ConfigFactory.fromMap(Map.of(
                VAULT_URL, URL,
                VAULT_TOKEN, TOKEN,
                VAULT_TOKEN_SCHEDULED_RENEW_ENABLED, "false"
        ));
        when(context.getConfig()).thenReturn(config);
        var ext = factory.constructInstance(HashicorpVaultExtension.class);
        ext.initialize(context);
        ext.start();
        verify(executorInstrumentation, never()).instrument(any(), anyString());
        verify(scheduledExecutorService, never()).execute(any());
    }

    @Test
    void shutdown_withTokenRenewTaskRunning_shouldStopTokenRenewTask(ServiceExtensionContext context) {
        extension.initialize(context);
        extension.start();
        verify(executorInstrumentation).instrument(any(), anyString());
        verify(scheduledExecutorService).execute(any());
        extension.shutdown();
        verify(scheduledExecutorService).shutdownNow();
    }

    @Test
    void shutdown_withTokenRenewTaskNotRunning_shouldNotStopTokenRenewTask(ServiceExtensionContext context) {
        extension.initialize(context);
        extension.shutdown();
        verify(scheduledExecutorService, never()).shutdownNow();
    }
}
