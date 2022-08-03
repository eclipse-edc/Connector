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
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.temporal.ChronoUnit;

@Provides({ Vault.class, PrivateKeyResolver.class })
public class HashicorpVaultExtension implements ServiceExtension {

    @EdcSetting(value = "The URL of the Hashicorp Vault", required = true)
    public static final String VAULT_URL = "edc.vault.hashicorp.url";

    @EdcSetting(value = "The token used to access the Hashicorp Vault", required = true)
    public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

    @EdcSetting(value = "The number of retries when accessing the Hashicorp Vault")
    public static final String MAX_RETRIES = "edc.core.retry.retries.max";

    @EdcSetting(value = "The minimum backoff in accessing the Hashicorp Vault")
    public static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";

    @EdcSetting(value = "The maximum backoff in accessing the Hashicorp Vault")
    public static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";

    private Vault vault;

    private PrivateKeyResolver privateKeyResolver;

    @Override
    public String name() {
        return "Hashicorp Vault";
    }

    @Provider
    public Vault vault() {
        return vault;
    }

    @Provider
    public PrivateKeyResolver privateKeyResolver() {
        return privateKeyResolver;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = loadHashicorpVaultClientConfig(context);

        var maxRetries = context.getSetting(MAX_RETRIES, 5);
        var minBackoff = context.getSetting(BACKOFF_MIN_MILLIS, 500);
        var maxBackoff = context.getSetting(BACKOFF_MAX_MILLIS, 10_000);

        var retryPolicy = RetryPolicy.builder()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS)
                .build();

        var okHttpClient = new OkHttpClient.Builder().build();
        var client = new Client(config, okHttpClient, context.getTypeManager(), retryPolicy);

        vault = new HashicorpVault(client, context.getMonitor());
        privateKeyResolver = new VaultPrivateKeyResolver(vault);
    }

    private Config loadHashicorpVaultClientConfig(
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

        return Config.Builder.newInstance()
                .vaultUrl(vaultUrl)
                .vaultToken(vaultToken)
                .build();
    }
}
