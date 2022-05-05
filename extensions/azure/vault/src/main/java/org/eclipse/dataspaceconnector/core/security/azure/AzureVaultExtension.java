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

package org.eclipse.dataspaceconnector.core.security.azure;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

import static org.eclipse.dataspaceconnector.common.string.StringUtils.isNullOrEmpty;


public class AzureVaultExtension implements VaultExtension {

    @EdcSetting
    private static final String VAULT_CLIENT_ID = "edc.vault.clientid";

    @EdcSetting
    private static final String VAULT_TENANT_ID = "edc.vault.tenantid";

    @EdcSetting
    private static final String VAULT_NAME = "edc.vault.name";

    @EdcSetting
    private static final String VAULT_CLIENT_SECRET = "edc.vault.clientsecret";

    @EdcSetting
    private static final String VAULT_CERTIFICATE = "edc.vault.certificate";

    private Vault vault;

    @Override
    public String name() {
        return "Azure Vault";
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return new VaultPrivateKeyResolver(vault);
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return new AzureCertificateResolver(vault);
    }

    @Override
    public void initialize(Monitor monitor, Config config) {
        var clientId = getMandatorySetting(config, VAULT_CLIENT_ID);
        var tenantId = getMandatorySetting(config, VAULT_TENANT_ID);
        var keyVaultName = getMandatorySetting(config, VAULT_NAME);

        var clientSecret = config.getString(VAULT_CLIENT_SECRET, null);
        var certPath = config.getString(VAULT_CERTIFICATE, null);
        if (isNullOrEmpty(certPath) && isNullOrEmpty(clientSecret)) {
            throw new AzureVaultException(String.format("Either '%s' or '%s' must be supplied but both were null", VAULT_CERTIFICATE, VAULT_CLIENT_SECRET));
        }

        vault = (certPath != null) ? AzureVault.authenticateWithCertificate(monitor, clientId, tenantId, certPath, keyVaultName) :
                AzureVault.authenticateWithSecret(monitor, clientId, tenantId, clientSecret, keyVaultName);

        monitor.info("AzureVaultExtension: authentication/initialization complete.");
    }

    private String getMandatorySetting(Config config, String setting) {
        try {
            return config.getString(setting);
        } catch (Exception e) {
            throw new AzureVaultException(e.getMessage());
        }
    }
}
