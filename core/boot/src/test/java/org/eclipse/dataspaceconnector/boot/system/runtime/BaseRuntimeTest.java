/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.boot.system.runtime;

import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseRuntimeTest {

    private final ServiceLocator serviceLocator = mock(ServiceLocator.class);
    private final ServiceExtensionContext context = mock(ServiceExtensionContext.class);
    private final BaseRuntime runtime = new BaseRuntime(serviceLocator);

    @BeforeEach
    void setUp() {
        when(context.getMonitor()).thenReturn(mock(Monitor.class));
    }

    @Test
    void initializeVault_registersServices() {
        when(serviceLocator.loadSingletonImplementor(VaultExtension.class, false)).thenReturn(null);

        runtime.initializeVault(context);

        verify(context).registerService(eq(Vault.class), any());
        verify(context).registerService(eq(PrivateKeyResolver.class), any());
        verify(context).registerService(eq(CertificateResolver.class), any());
    }

    @Test
    void initializeVault_asSingletonInstance() {
        var vault = mock(Vault.class);
        var privateKeyResolver = mock(PrivateKeyResolver.class);
        var certificateResolver = mock(CertificateResolver.class);
        when(serviceLocator.loadSingletonImplementor(VaultExtension.class, false))
                .thenReturn(new TestVaultExtension(vault, privateKeyResolver, certificateResolver));

        runtime.initializeVault(context);

        verify(context).registerService(eq(Vault.class), same(vault));
        verify(context).registerService(eq(PrivateKeyResolver.class), same(privateKeyResolver));
        verify(context).registerService(eq(CertificateResolver.class), same(certificateResolver));
    }

    private static class TestVaultExtension implements VaultExtension {
        private final Vault vault;
        private final PrivateKeyResolver privateKeyResolver;
        private final CertificateResolver certificateResolver;

        TestVaultExtension(Vault vault, PrivateKeyResolver privateKeyResolver, CertificateResolver certificateResolver) {
            this.vault = vault;
            this.privateKeyResolver = privateKeyResolver;
            this.certificateResolver = certificateResolver;
        }

        @Override
        public Vault getVault() {
            return vault;
        }

        @Override
        public PrivateKeyResolver getPrivateKeyResolver() {
            return privateKeyResolver;
        }

        @Override
        public CertificateResolver getCertificateResolver() {
            return certificateResolver;
        }
    }

}