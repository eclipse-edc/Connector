/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.azure;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
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

    private AzureVault(TokenCredential credential, Monitor monitor, String keyVaultUri) {
        this.monitor = monitor;
        secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(credential)
                .buildClient();
    }

    public static AzureVault authenticateWithSecret(Monitor monitor, String clientId, String tenantId, String clientSecret, String keyVaultName) {
        String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

        TokenCredential credential = new ClientSecretCredentialBuilder().clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build();

        return new AzureVault(credential, monitor, keyVaultUri);
    }

    public static AzureVault authenticateWithCertificate(Monitor monitor, String clientId, String tenantId, String certificatePath, String keyVaultName) {
        String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

        TokenCredential credential = new ClientCertificateCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .pfxCertificate(certificatePath, "")
                .build();

        return new AzureVault(credential, monitor, keyVaultUri);
    }


    @Override
    public @Nullable String resolveSecret(String key) {
        try {

            key = sanitizeKey(key);
            var secret = secretClient.getSecret(key);
            return secret.getValue();
        } catch (ResourceNotFoundException ex) {
            monitor.severe("Secret not found!", ex);
            return null;
        }

    }

    @NotNull
    private String sanitizeKey(String key) {
        if (key.contains(".")) {
            monitor.info("AzureVault: key contained '.' which is not allowed. replaced with '-'");
            key = key.replace(".", "-");
        }
        return key;
    }

    @Override
    public VaultResponse storeSecret(String key, String value) {
        try {
            key = sanitizeKey(key);
            var secret = secretClient.setSecret(key, value);
            monitor.debug("storing secret successful");
            return VaultResponse.OK;
        } catch (Exception ex) {
            monitor.severe("Error storing secret", ex);
            return new VaultResponse(ex.getMessage());
        }
    }

    @Override
    public VaultResponse deleteSecret(String key) {
        key = sanitizeKey(key);
        SyncPoller<DeletedSecret, Void> poller = null;
        try {
            poller = secretClient.beginDeleteSecret(key);
            monitor.debug("Begin deleting secret");
            poller.waitForCompletion(Duration.ofMinutes(1));

            monitor.debug("deletion complete");
            return VaultResponse.OK;
        } catch (ResourceNotFoundException ex) {
            monitor.severe("Error deleting secret - does not exist!");
            return new VaultResponse(ex.getMessage());
        } catch (RuntimeException re) {
            monitor.severe("Error deleting secret", re);

            if (re.getCause() != null && re.getCause() instanceof TimeoutException) {
                try {
                    if (poller != null) {
                        poller.cancelOperation();
                    }
                } catch (Exception e) {
                    monitor.severe("Failed to abort the deletion. ", e);
                    return new VaultResponse(e.getMessage());
                }
            }
            return new VaultResponse(re.getMessage());
        } finally {
            try {
                secretClient.purgeDeletedSecret(key);
            } catch (Exception e) {
                monitor.severe("Error purging secret from AzureVault", e);
            }
        }
    }
}
