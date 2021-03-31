package com.microsoft.dagx.system;

import com.microsoft.dagx.monitor.ConsoleMonitor;
import com.microsoft.dagx.spi.monitor.MultiplexingMonitor;
import com.microsoft.dagx.security.NullVaultExtension;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.MonitorExtension;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.system.VaultExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ExtensionLoader {

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
        context.registerService(Vault.class, vaultExtension.getVault());
        context.registerService(PrivateKeyResolver.class, vaultExtension.getPrivateKeyResolver());
        context.registerService(CertificateResolver.class, vaultExtension.getCertificateResolver());
    }

    public static @NotNull Monitor loadMonitor(){
        var loader= ServiceLoader.load(MonitorExtension.class);
        return loadMonitor(loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList()));
    }

    static @NotNull Monitor loadMonitor(List<MonitorExtension> availableMonitors) {


        if(availableMonitors.isEmpty())
            return new ConsoleMonitor();

        if (availableMonitors.size() > 1) {
            return new MultiplexingMonitor(availableMonitors.stream().map(MonitorExtension::getMonitor).collect(Collectors.toList()));
        }

        return availableMonitors.get(0).getMonitor();
    }

    private ExtensionLoader() {
    }
}
