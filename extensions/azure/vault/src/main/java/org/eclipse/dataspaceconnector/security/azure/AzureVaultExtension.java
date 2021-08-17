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

package org.eclipse.dataspaceconnector.security.azure;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;

import static org.eclipse.dataspaceconnector.common.string.StringUtils.isNullOrEmpty;


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
