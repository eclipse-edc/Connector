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

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Implements a vault backed by Azure Vault.
 */
public class AzureVault implements Vault {

    private final SecretClient secretClient;
    private final Monitor monitor;

    public static AzureVault authenticateWithSecret(Monitor monitor, String clientId, String tenantId, String clientSecret, String keyVaultName) {
        var credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .build();

        return new AzureVault(monitor, createSecretClient(credential, keyVaultName));
    }

    public static AzureVault authenticateWithCertificate(Monitor monitor, String clientId, String tenantId, String certificatePath, String keyVaultName) {
        var credential = new ClientCertificateCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .pfxCertificate(certificatePath, "")
                .build();

        return new AzureVault(monitor, createSecretClient(credential, keyVaultName));
    }

    protected AzureVault(Monitor monitor, SecretClient secretClient) {
        this.monitor = monitor;
        this.secretClient = secretClient;
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        try {
            var sanitizedKey = sanitizeKey(key);
            var secret = secretClient.getSecret(sanitizedKey);
            return secret.getValue();
        } catch (ResourceNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            monitor.severe("Error accessing secret:", ex);
            return null;
        }
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        try {
            var sanitizedKey = sanitizeKey(key);
            var secret = secretClient.setSecret(sanitizedKey, value);
            monitor.debug("storing secret successful");
            return Result.success();
        } catch (Exception ex) {
            monitor.severe("Error storing secret", ex);
            return Result.failure(ex.getMessage());
        }
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        var sanitizedKey = sanitizeKey(key);
        SyncPoller<DeletedSecret, Void> poller = null;
        try {
            poller = secretClient.beginDeleteSecret(sanitizedKey);
            monitor.debug("Begin deleting secret");
            poller.waitForCompletion(Duration.ofMinutes(1));

            monitor.debug("deletion complete");
            return Result.success();
        } catch (ResourceNotFoundException ex) {
            monitor.severe("Error deleting secret - does not exist!");
            return Result.failure(ex.getMessage());
        } catch (RuntimeException re) {
            monitor.severe("Error deleting secret", re);

            if (re.getCause() != null && re.getCause() instanceof TimeoutException) {
                try {
                    if (poller != null) {
                        poller.cancelOperation();
                    }
                } catch (Exception e) {
                    monitor.severe("Failed to abort the deletion. ", e);
                    return Result.failure(e.getMessage());
                }
            }
            return Result.failure(re.getMessage());
        } finally {
            try {
                secretClient.purgeDeletedSecret(sanitizedKey);
            } catch (Exception e) {
                monitor.severe("Error purging secret from AzureVault", e);
            }
        }
    }

    @NotNull
    private String sanitizeKey(String key) {
        if (key.contains(".")) {
            monitor.debug("AzureVault: key contained '.' which is not allowed. replaced with '-'");
            return key.replace(".", "-");
        }
        return key;
    }

    @NotNull
    private static SecretClient createSecretClient(TokenCredential credential, String keyVaultName) {
        return new SecretClientBuilder()
                .vaultUrl("https://" + keyVaultName + ".vault.azure.net")
                .credential(credential)
                .buildClient();
    }
}
