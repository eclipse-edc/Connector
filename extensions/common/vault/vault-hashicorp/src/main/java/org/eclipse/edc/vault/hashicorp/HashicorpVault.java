/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.vault.hashicorp.HashicorpVaultSettings.forParticipant;

/**
 * Vault implementation for Hashicorp Vault. The actual interaction with the vault is delegated to a {@link HashicorpVaultClient} via HTTP.
 * Each vault partition is mapped to its own {@link HashicorpVaultClient} instance with specific configuration / authentication settings, taken
 * from a {@link ParticipantContextConfig}. So theoretically, each participant context's vault could be a separate instance.
 */
class HashicorpVault implements Vault {
    private final ParticipantContextConfig participantContextConfig;
    private final Monitor monitor;
    private final HashicorpVaultConfig vaultConfig;
    private final HashicorpVaultTokenProvider defaultTokenProvider;
    private final EdcHttpClient edcHttpClient;
    private final ObjectMapper mapper;

    HashicorpVault(ParticipantContextConfig participantContextConfig,
                   Monitor monitor,
                   HashicorpVaultConfig vaultConfig, HashicorpVaultTokenProvider defaultTokenProvider,
                   EdcHttpClient edcHttpClient) {
        this.participantContextConfig = participantContextConfig;
        this.monitor = monitor;
        this.vaultConfig = vaultConfig;
        this.defaultTokenProvider = defaultTokenProvider;
        this.edcHttpClient = edcHttpClient;
        this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return resolveSecret(null, key);
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        return storeSecret(null, key, value);
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return deleteSecret(null, key);
    }

    @Override
    public String resolveSecret(String vaultPartition, String key) {

        return getVaultClient(vaultPartition)
                .resolveSecret(key);
    }

    @Override
    public Result<Void> storeSecret(String vaultPartition, String key, String value) {
        return getVaultClient(vaultPartition)
                .storeSecret(key, value);
    }

    @Override
    public Result<Void> deleteSecret(String vaultPartition, String key) {
        return getVaultClient(vaultPartition)
                .deleteSecret(key);
    }

    private @NotNull HashicorpVaultClient getVaultClient(String vaultPartition) {
        if (vaultPartition == null) {
            return createDefault();
        }
        var client = createForPartition(vaultPartition);
        if (client == null) {
            if (vaultConfig.isAllowFallback()) {
                return createDefault();
            }
            throw new IllegalArgumentException("No vault config found for partition '%s' and falling back to the default vault is not allowed".formatted(vaultPartition));
        }
        return client;
    }

    /**
     * creates a new HashicorpVaultClient specific configuration / auth settings for the given vault partition.
     * If no vault config is found for the given partition, null is returned.
     */
    private @Nullable HashicorpVaultClient createForPartition(String vaultPartition) {
        var settings = forParticipant(vaultPartition, participantContextConfig);
        if (settings == null) {
            return null;
        }
        return new HashicorpVaultClient(monitor, settings.config(), edcHttpClient, mapper, settings.tokenProvider(edcHttpClient));
    }

    /**
     * creates a new HashicorpVaultClient with the "global" configuration / auth-settings taken from the runtime configuration.
     */
    private HashicorpVaultClient createDefault() {
        return new HashicorpVaultClient(monitor, vaultConfig, edcHttpClient, mapper, defaultTokenProvider);
    }

}
