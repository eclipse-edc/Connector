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

package org.eclipse.edc.participantcontext.config.service;

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class ParticipantContextConfigServiceImpl implements ParticipantContextConfigService {

    private final EncryptionAlgorithmRegistry encryptionRegistry;
    private final String encryptionAlgorithm;
    private final ParticipantContextConfigStore configStore;
    private final TransactionContext transactionContext;
    private final Clock clock;

    public ParticipantContextConfigServiceImpl(EncryptionAlgorithmRegistry encryptionRegistry, String encryptionAlgorithm, ParticipantContextConfigStore configStore, TransactionContext transactionContext, Clock clock) {
        this.encryptionRegistry = encryptionRegistry;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.configStore = configStore;
        this.transactionContext = transactionContext;
        this.clock = clock;
    }

    @Override
    public ServiceResult<Void> save(ParticipantContextConfiguration config) {
        return transactionContext.execute(() -> {
            if (hasNullValue(config.getEntries()) || hasNullValue(config.getPrivateEntries())) {
                return ServiceResult.badRequest("Null values are not allowed when setting a configuration");
            }
            return encryptEntries(config)
                    .onSuccess(configStore::save)
                    .flatMap(ServiceResult::from)
                    .mapEmpty();
        });
    }

    @Override
    public ServiceResult<Void> merge(ParticipantContextConfiguration config) {
        return transactionContext.execute(() -> {
            var existing = configStore.get(config.getParticipantContextId());
            return ServiceResult.from(encryptEntries(config))
                    .map(encryptedPatch -> {
                        var base = existing != null
                                ? existing
                                : ParticipantContextConfiguration.Builder.newInstance()
                                    .participantContextId(config.getParticipantContextId())
                                    .createdAt(clock.millis())
                                    .build();
                        return base.toBuilder()
                                .entries(mergePatch(base.getEntries(), encryptedPatch.getEntries()))
                                .privateEntries(mergePatch(base.getPrivateEntries(), encryptedPatch.getPrivateEntries()))
                                .lastModified(clock.millis())
                                .build();
                    })
                    .onSuccess(configStore::save)
                    .mapEmpty();
        });
    }

    private static boolean hasNullValue(Map<String, String> map) {
        return map.values().stream().anyMatch(value -> value == null);
    }

    /**
     * Applies a JSON Merge Patch (RFC 7396) of {@code patch} onto {@code base}: a null value removes the key, any
     * other value adds or overwrites it.
     */
    private static Map<String, String> mergePatch(Map<String, String> base, Map<String, String> patch) {
        var merged = new HashMap<>(base);
        patch.forEach((key, value) -> {
            if (value == null) {
                merged.remove(key);
            } else {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private Result<ParticipantContextConfiguration> encryptEntries(ParticipantContextConfiguration config) {
        var encryptedPrivateEntries = new HashMap<String, String>();
        for (var entry : config.getPrivateEntries().entrySet()) {
            if (entry.getValue() == null) {
                // a null value is a removal signal (RFC 7396), keep it as-is instead of encrypting
                encryptedPrivateEntries.put(entry.getKey(), null);
                continue;
            }
            var result = encryptionRegistry.encrypt(encryptionAlgorithm, entry.getValue());
            if (result.failed()) {
                return Result.failure("Failed to encrypt entries: " + result.getFailureDetail());
            }
            encryptedPrivateEntries.put(entry.getKey(), result.getContent());
        }
        return Result.success(config.toBuilder().privateEntries(encryptedPrivateEntries).build());
    }

    @Override
    public ServiceResult<ParticipantContextConfiguration> get(String participantContextId) {
        return transactionContext.execute(() -> {
            var config = configStore.get(participantContextId);
            if (config == null) {
                return ServiceResult.notFound("No configuration found for participant context with id " + participantContextId);
            }
            return ServiceResult.success(config);
        });
    }
}
