package com.microsoft.dagx.system;

import com.microsoft.dagx.monitor.ConsoleMonitor;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.monitor.MultiplexingMonitor;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.MonitorExtension;
import com.microsoft.dagx.spi.system.VaultExtension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.easymock.EasyMock.*;
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
        contextMock.registerService(eq(PrivateKeyResolver.class), anyObject()); //these are not null but a lambda returning null
        contextMock.registerService(eq(CertificateResolver.class), anyObject());
        replay(contextMock);

        ExtensionLoader.loadVault(contextMock);
        verify(contextMock);

    }

    @Test
    void loadVault() {
        DefaultServiceExtensionContext contextMock = niceMock(DefaultServiceExtensionContext.class);
        Vault vaultMock = mock(Vault.class);
        PrivateKeyResolver resolverMock = mock(PrivateKeyResolver.class);
        CertificateResolver certResolverMock = mock(CertificateResolver.class);
        expect(contextMock.loadSingletonExtension(VaultExtension.class, false)).andReturn(new VaultExtension() {

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

        contextMock.registerService(Vault.class, vaultMock);
        expectLastCall().once();


        replay(contextMock);
        ExtensionLoader.loadVault(contextMock);
        verify(contextMock);

    }
}
