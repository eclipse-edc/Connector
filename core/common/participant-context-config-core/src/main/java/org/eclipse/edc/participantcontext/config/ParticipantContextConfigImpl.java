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

package org.eclipse.edc.participantcontext.config;

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * Default implementation of {@link ParticipantContextConfig} backed by the {@link ParticipantContextConfigStore}.
 * <p>
 * Participant configurations are cached for the duration of the enclosing transaction: within a single transaction
 * the store is queried at most once per participant context, regardless of how many settings are read. The cache is
 * cleared when the transaction completes. When no enclosing transaction is active, every call hits the store.
 * <p>
 * Note: changes to a participant configuration made within the same transaction after it has been read (e.g. via
 * {@link org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService}) are not visible to
 * subsequent reads in that transaction.
 */
public class ParticipantContextConfigImpl implements ParticipantContextConfig {


    private final EncryptionAlgorithmRegistry registry;
    private final String encryptionAlgorithm;
    private final ParticipantContextConfigStore configStore;
    private final TransactionContext transactionContext;
    private final ThreadLocal<Map<String, Optional<ParticipantContextConfiguration>>> cache = new ThreadLocal<>();


    public ParticipantContextConfigImpl(EncryptionAlgorithmRegistry registry, String encryptionAlgorithm, ParticipantContextConfigStore configStore, TransactionContext transactionContext) {
        this.registry = registry;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.configStore = configStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public String getString(String participantContextId, String key) {
        return config(participantContextId).getString(key);
    }

    @Override
    public String getString(String participantContextId, String key, String defaultValue) {
        return configOrEmpty(participantContextId).getString(key, defaultValue);
    }

    @Override
    public Integer getInteger(String participantContextId, String key) {
        return config(participantContextId).getInteger(key);
    }

    @Override
    public Integer getInteger(String participantContextId, String key, Integer defaultValue) {
        return configOrEmpty(participantContextId).getInteger(key, defaultValue);
    }

    @Override
    public Long getLong(String participantContextId, String key) {
        return config(participantContextId).getLong(key);

    }

    @Override
    public Long getLong(String participantContextId, String key, Long defaultValue) {
        return configOrEmpty(participantContextId).getLong(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String participantContextId, String key) {
        return config(participantContextId).getBoolean(key);
    }

    @Override
    public Boolean getBoolean(String participantContextId, String key, Boolean defaultValue) {
        return configOrEmpty(participantContextId).getBoolean(key, defaultValue);
    }

    @Override
    public String getSensitiveString(String participantContextId, String key) {
        var encryptedValue = privateConfig(participantContextId).getString(key, null);
        if (encryptedValue == null) {
            return null;
        }
        return registry.decrypt(encryptionAlgorithm, encryptedValue)
                .orElseThrow(f -> new EdcException(format("Failed to decrypt sensitive config value for key %s and participant context %s", key, participantContextId)));
    }

    private Config config(String participantContextId) {
        return fetchConfig(participantContextId, ParticipantContextConfiguration::getEntries)
                .orElseThrow(() -> new EdcException("No configuration found for participant context " + participantContextId));
    }

    /**
     * Used by getters with a default value: a missing participant configuration is treated as an empty one, so that the
     * default value is returned.
     */
    private Config configOrEmpty(String participantContextId) {
        return fetchConfig(participantContextId, ParticipantContextConfiguration::getEntries)
                .orElseGet(ConfigFactory::empty);
    }

    private Config privateConfig(String participantContextId) {
        return fetchConfig(participantContextId, ParticipantContextConfiguration::getPrivateEntries)
                .orElseThrow(() -> new EdcException("No configuration found for participant context " + participantContextId));
    }

    private Optional<Config> fetchConfig(String participantContextId, Function<ParticipantContextConfiguration, Map<String, String>> supplier) {
        return transactionContext.execute(() -> transactionCache()
                .computeIfAbsent(participantContextId, id -> Optional.ofNullable(configStore.get(id)))
                .map(cfg -> ConfigFactory.fromMap(supplier.apply(cfg))));
    }

    /**
     * Returns the cache bound to the current transaction, creating it and registering its cleanup on the first access.
     * Must be called within a transaction block.
     */
    private Map<String, Optional<ParticipantContextConfiguration>> transactionCache() {
        var configurations = cache.get();
        if (configurations == null) {
            configurations = new HashMap<>();
            cache.set(configurations);
            transactionContext.registerSynchronization(cache::remove);
        }
        return configurations;
    }

}
