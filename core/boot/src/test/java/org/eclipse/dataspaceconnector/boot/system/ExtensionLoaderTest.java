/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.core.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.monitor.MultiplexingMonitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.MonitorExtension;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtensionLoaderTest {

    @Test
    void loadMonitor_whenSingleMonitorExtension() {
        var mockedMonitor = mock(Monitor.class);
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mockedMonitor);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertEquals(mockedMonitor, monitor);
    }

    @Test
    void loadMonitor_whenMultipleMonitorExtensions() {
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mock(Monitor.class));
        exts.add(ConsoleMonitor::new);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertTrue(monitor instanceof MultiplexingMonitor);
    }

    @Test
    void loadMonitor_whenNoMonitorExtension() {
        var monitor = ExtensionLoader.loadMonitor(new ArrayList<>());

        assertTrue(monitor instanceof ConsoleMonitor);
    }

    @Test
    void loadVault_whenNotRegistered() {
        DefaultServiceExtensionContext contextMock = mock(DefaultServiceExtensionContext.class);

        when(contextMock.getMonitor()).thenReturn(mock(Monitor.class));
        when(contextMock.loadSingletonExtension(VaultExtension.class, false)).thenReturn(null);

        ExtensionLoader.loadVault(contextMock);

        verify(contextMock).registerService(eq(Vault.class), isA(Vault.class));
        verify(contextMock).registerService(eq(PrivateKeyResolver.class), any());
        verify(contextMock).registerService(eq(CertificateResolver.class), any());
        verify(contextMock, atLeastOnce()).getMonitor();
        verify(contextMock).loadSingletonExtension(VaultExtension.class, false);
    }

    @Test
    void loadVault() {
        DefaultServiceExtensionContext contextMock = mock(DefaultServiceExtensionContext.class);
        Vault vaultMock = mock(Vault.class);
        PrivateKeyResolver resolverMock = mock(PrivateKeyResolver.class);
        CertificateResolver certResolverMock = mock(CertificateResolver.class);
        when(contextMock.getMonitor()).thenReturn(mock(Monitor.class));
        when(contextMock.loadSingletonExtension(VaultExtension.class, false)).thenReturn(new VaultExtension() {

            @Override
            public Vault getVault() {
                return vaultMock;
            }

            @Override
            public PrivateKeyResolver getPrivateKeyResolver() {
                return resolverMock;
            }

            @Override
            public CertificateResolver getCertificateResolver() {
                return certResolverMock;
            }
        });

        ExtensionLoader.loadVault(contextMock);

        verify(contextMock, times(1)).registerService(Vault.class, vaultMock);
        verify(contextMock, atLeastOnce()).getMonitor();
        verify(contextMock).loadSingletonExtension(VaultExtension.class, false);
    }
}
