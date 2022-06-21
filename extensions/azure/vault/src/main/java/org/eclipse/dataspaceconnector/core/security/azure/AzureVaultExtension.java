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
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static org.eclipse.dataspaceconnector.common.string.StringUtils.isNullOrEmpty;

@Provides({ Vault.class, PrivateKeyResolver.class, CertificateResolver.class })
public class AzureVaultExtension implements ServiceExtension {

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

    @Override
    public String name() {
        return "Azure Vault";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var clientId = getMandatorySetting(context, VAULT_CLIENT_ID);
        var tenantId = getMandatorySetting(context, VAULT_TENANT_ID);
        var keyVaultName = getMandatorySetting(context, VAULT_NAME);

        var clientSecret = context.getSetting(VAULT_CLIENT_SECRET, null);
        var certPath = context.getSetting(VAULT_CERTIFICATE, null);
        if (isNullOrEmpty(certPath) && isNullOrEmpty(clientSecret)) {
            throw new AzureVaultException(String.format("Either '%s' or '%s' must be supplied but both were null", VAULT_CERTIFICATE, VAULT_CLIENT_SECRET));
        }

        var vault = (certPath != null)
                ? AzureVault.authenticateWithCertificate(context.getMonitor(), clientId, tenantId, certPath, keyVaultName)
                : AzureVault.authenticateWithSecret(context.getMonitor(), clientId, tenantId, clientSecret, keyVaultName);

        context.getMonitor().info("AzureVaultExtension: authentication/initialization complete.");

        context.registerService(Vault.class, vault);
        context.registerService(PrivateKeyResolver.class, new VaultPrivateKeyResolver(vault));
        context.registerService(CertificateResolver.class, new AzureCertificateResolver(vault));
    }

    private String getMandatorySetting(ServiceExtensionContext context, String setting) {
        var value = context.getSetting(setting, null);
        if (isNullOrEmpty(setting)) {
            throw new AzureVaultException(String.format("'%s' must be supplied but was null", setting));
        }
        return value;
    }

}
