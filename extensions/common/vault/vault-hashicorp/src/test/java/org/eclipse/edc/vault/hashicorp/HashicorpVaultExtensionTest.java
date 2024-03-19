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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultExtension.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultExtension.VAULT_URL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {


    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        var healthCheckService = mock(HealthCheckService.class);
        context.registerService(HealthCheckService.class, healthCheckService);
    }

    @Test
    void createVault_whenNoVaultUrl_expectException(ServiceExtensionContext context, HashicorpVaultExtension extension) {
        when(context.getSetting(VAULT_URL, null)).thenReturn(null);

        assertThatThrownBy(() -> extension.hashicorpVault(context)).isInstanceOf(EdcException.class);
    }

    @Test
    void createVault_whenNoVaultToken_expectException(ServiceExtensionContext context, HashicorpVaultExtension extension) {
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn(null);
        when(context.getSetting(VAULT_URL, null)).thenReturn("https://some.vault");

        assertThrows(EdcException.class, () -> extension.hashicorpVault(context));
    }

    @Test
    void createVault_ensureType(HashicorpVaultExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(VAULT_URL, null)).thenReturn("https://some.vault");
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn("some-token");


        assertThat(extension.hashicorpVault(context)).isInstanceOf(HashicorpVault.class);
    }
}
