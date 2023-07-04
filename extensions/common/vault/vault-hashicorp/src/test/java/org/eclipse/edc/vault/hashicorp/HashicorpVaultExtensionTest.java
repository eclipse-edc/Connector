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

import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_URL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {

    private HashicorpVaultExtension extension;

    // mocks
    private ServiceExtensionContext context;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        var healthCheckService = mock(HealthCheckService.class);
        context.registerService(HealthCheckService.class, healthCheckService);
        extension = factory.constructInstance(HashicorpVaultExtension.class);
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultUrlUndefined() {
        when(context.getSetting(VAULT_URL, null)).thenReturn(null);

        assertThrows(EdcException.class, () -> extension.initialize(context));
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultTokenUndefined() {
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn(null);
        assertThrows(EdcException.class, () -> extension.initialize(context));
    }
}
