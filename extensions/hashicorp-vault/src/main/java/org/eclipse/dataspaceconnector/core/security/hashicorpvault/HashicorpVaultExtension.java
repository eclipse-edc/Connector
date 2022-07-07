/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides({Vault.class, PrivateKeyResolver.class, CertificateResolver.class})
public class HashicorpVaultExtension implements ServiceExtension {

    @EdcSetting(required = true)
    public static final String VAULT_URL = "edc.vault.hashicorp.url";

    @EdcSetting(required = true)
    public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

    @Override
    public String name() {
        return "Hashicorp Vault";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = loadHashicorpVaultClientConfig(context);

        var retryPolicy = context.getService(RetryPolicy.class);
        var okHttpClient = new OkHttpClient.Builder().build();
        var client = new HashicorpVaultClient(config, okHttpClient, context.getTypeManager(), retryPolicy);

        var vault = new HashicorpVault(client, context.getMonitor());
        var certificateResolver = new HashicorpCertificateResolver(vault, context.getMonitor());
        var privateKeyResolver = new VaultPrivateKeyResolver(vault);

        context.registerService(Vault.class, vault);
        context.registerService(CertificateResolver.class, certificateResolver);
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
    }

    private HashicorpVaultClientConfig loadHashicorpVaultClientConfig(
            ServiceExtensionContext context) {

        var vaultUrl = context.getSetting(VAULT_URL, null);
        if (vaultUrl == null) {
            throw new EdcException(String.format("Vault URL (%s) must be defined", VAULT_URL));
        }

        var vaultToken = context.getSetting(VAULT_TOKEN, null);

        if (vaultToken == null) {
            throw new EdcException(
                    String.format("For Vault authentication [%s] is required", VAULT_TOKEN));
        }

        return HashicorpVaultClientConfig.Builder.newInstance()
                .vaultUrl(vaultUrl)
                .vaultToken(vaultToken)
                .build();
    }
}
