package com.microsoft.dagx.system;

import com.microsoft.dagx.security.NullVaultExtension;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.system.VaultExtension;

import java.util.List;

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

    private ExtensionLoader() {
    }
}
