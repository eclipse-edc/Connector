/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.azure;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.system.VaultExtension;

import static com.microsoft.dagx.common.string.StringUtils.isNullOrEmpty;


public class AzureVaultExtension implements VaultExtension {

    private Vault vault;

    @Override
    public void initialize(Monitor monitor) {
        monitor.debug("AzureVaultExtension: general initialization complete");
    }

    @Override
    public void intializeVault(ServiceExtensionContext context) {
        String clientId = context.getSetting("dagx.vault.clientid", null);
        if (isNullOrEmpty(clientId)) {
            throw new AzureVaultException("'dagx.vault.clientid' must be supplied but was null!");
        }

        String tenantId = context.getSetting("dagx.vault.tenantid", null);
        if (isNullOrEmpty(tenantId)) {
            throw new AzureVaultException("'dagx.vault.tenantid' must be supplied but was null!");
        }

        String certPath = context.getSetting("dagx.vault.certificate", null);
        if (isNullOrEmpty(certPath)) {
            throw new AzureVaultException("'dagx.vault.certificate' must be supplied but was null!");
        }

        String keyVaultName = context.getSetting("dagx.vault.name", null);
        if (isNullOrEmpty(keyVaultName)) {
            throw new AzureVaultException("'dagx.vault.name' must be supplied but was null!");
        }

        vault = AzureVault.authenticateWithCertificate(context.getMonitor(), clientId, tenantId, certPath, keyVaultName);
        context.getMonitor().info("AzureVaultExtension: authentication/initialization complete.");
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return new AzurePrivateKeyResolver(vault);
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return new AzureCertificateResolver(vault);
    }
}
