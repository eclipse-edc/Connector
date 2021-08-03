/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.security.azure;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.VaultExtension;

import static org.eclipse.edc.common.string.StringUtils.isNullOrEmpty;


public class AzureVaultExtension implements VaultExtension {

    private Vault vault;

    @Override
    public void initialize(Monitor monitor) {
        monitor.debug("AzureVaultExtension: general initialization complete");
    }

    @Override
    public void intializeVault(ServiceExtensionContext context) {
        String clientId = context.getSetting("edc.vault.clientid", null);
        if (isNullOrEmpty(clientId)) {
            throw new AzureVaultException("'edc.vault.clientid' must be supplied but was null!");
        }

        String tenantId = context.getSetting("edc.vault.tenantid", null);
        if (isNullOrEmpty(tenantId)) {
            throw new AzureVaultException("'edc.vault.tenantid' must be supplied but was null!");
        }

        String certPath = context.getSetting("edc.vault.certificate", null);
        if (isNullOrEmpty(certPath)) {
            throw new AzureVaultException("'edc.vault.certificate' must be supplied but was null!");
        }

        String keyVaultName = context.getSetting("edc.vault.name", null);
        if (isNullOrEmpty(keyVaultName)) {
            throw new AzureVaultException("'edc.vault.name' must be supplied but was null!");
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
