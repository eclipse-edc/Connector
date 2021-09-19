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

package org.eclipse.dataspaceconnector.system;

import org.eclipse.dataspaceconnector.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.monitor.MultiplexingMonitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.RsaPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.MonitorExtension;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionLoaderTest {

    @Test
    void loadMonitor_whenSingleMonitorExtension() {
        var mockedMonitor = (Monitor) mock(Monitor.class);

        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mockedMonitor);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertEquals(mockedMonitor, monitor);
    }

    @Test
    void loadMonitor_whenMultipleMonitorExtensions() {
        var mockedMonitor = (Monitor) mock(Monitor.class);

        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mockedMonitor);
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

        DefaultServiceExtensionContext contextMock = niceMock(DefaultServiceExtensionContext.class);

        expect(contextMock.getMonitor()).andReturn(new Monitor() {
        });

        expect(contextMock.loadSingletonExtension(VaultExtension.class, false)).andReturn(null);
        contextMock.registerService(eq(Vault.class), isA(Vault.class));
        contextMock.registerService(eq(RsaPrivateKeyResolver.class), anyObject()); //these are not null but a lambda returning null
        contextMock.registerService(eq(CertificateResolver.class), anyObject());
        replay(contextMock);

        ExtensionLoader.loadVault(contextMock);
        verify(contextMock);

    }

    @Test
    void loadVault() {
        DefaultServiceExtensionContext contextMock = niceMock(DefaultServiceExtensionContext.class);
        Vault vaultMock = mock(Vault.class);
        RsaPrivateKeyResolver resolverMock = mock(RsaPrivateKeyResolver.class);
        CertificateResolver certResolverMock = mock(CertificateResolver.class);
        expect(contextMock.loadSingletonExtension(VaultExtension.class, false)).andReturn(new VaultExtension() {

            @Override
            public Vault getVault() {
                return vaultMock;
            }

            @Override
            public RsaPrivateKeyResolver getPrivateKeyResolver() {
                return resolverMock;
            }

            @Override
            public CertificateResolver getCertificateResolver() {
                return certResolverMock;
            }
        });

        contextMock.registerService(Vault.class, vaultMock);
        expectLastCall().once();


        replay(contextMock);
        ExtensionLoader.loadVault(contextMock);
        verify(contextMock);

    }
}
