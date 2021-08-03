/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.system;

import org.eclipse.edc.monitor.ConsoleMonitor;
import org.eclipse.edc.security.NullVaultExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.monitor.MultiplexingMonitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.VaultExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ExtensionLoader {

    private ExtensionLoader() {
    }

    /**
     * Convenience method for loading service extensions.
     */
    public static void bootServiceExtensions(List<ServiceExtension> serviceExtensions, ServiceExtensionContext context) {
        serviceExtensions.forEach(extension -> extension.initialize(context));
        serviceExtensions.forEach(ServiceExtension::start);
    }

    /**
     * Loads a vault extension.
     */
    public static void loadVault(DefaultServiceExtensionContext context) {
        VaultExtension vaultExtension = context.loadSingletonExtension(VaultExtension.class, false);
        if (vaultExtension == null) {
            vaultExtension = new NullVaultExtension();
            context.getMonitor().info("Secrets vault not configured. Defaulting to null vault.");
        }
        vaultExtension.initialize(context.getMonitor());
        vaultExtension.intializeVault(context);
        context.registerService(Vault.class, vaultExtension.getVault());
        context.registerService(PrivateKeyResolver.class, vaultExtension.getPrivateKeyResolver());
        context.registerService(CertificateResolver.class, vaultExtension.getCertificateResolver());
    }

    public static @NotNull Monitor loadMonitor() {
        var loader = ServiceLoader.load(MonitorExtension.class);
        return loadMonitor(loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList()));
    }

    static @NotNull Monitor loadMonitor(List<MonitorExtension> availableMonitors) {


        if (availableMonitors.isEmpty()) {
            return new ConsoleMonitor();
        }

        if (availableMonitors.size() > 1) {
            return new MultiplexingMonitor(availableMonitors.stream().map(MonitorExtension::getMonitor).collect(Collectors.toList()));
        }

        return availableMonitors.get(0).getMonitor();
    }
}
