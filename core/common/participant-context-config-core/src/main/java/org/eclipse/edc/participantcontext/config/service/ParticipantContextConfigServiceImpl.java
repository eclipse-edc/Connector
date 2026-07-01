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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return transactionContext.execute(() -> encryptEntries(config)
                .onSuccess(configStore::save)
                .flatMap(ServiceResult::from)
                .mapEmpty());
    }

    @Override
    public ServiceResult<Void> merge(ParticipantContextConfiguration config) {
        return transactionContext.execute(() -> {
            var existing = configStore.get(config.getParticipantContextId());
            if (existing == null) {
                return ServiceResult.notFound("No configuration found for participant context with id " + config.getParticipantContextId());
            }
            return ServiceResult.from(encryptEntries(config))
                    .map(encryptedPatch -> {
                        var mergedEntries = new HashMap<>(existing.getEntries());
                        mergedEntries.putAll(encryptedPatch.getEntries());
                        var mergedPrivateEntries = new HashMap<>(existing.getPrivateEntries());
                        mergedPrivateEntries.putAll(encryptedPatch.getPrivateEntries());
                        return existing.toBuilder()
                                .entries(mergedEntries)
                                .privateEntries(mergedPrivateEntries)
                                .lastModified(clock.millis())
                                .build();
                    })
                    .onSuccess(configStore::save)
                    .mapEmpty();
        });
    }


    private Result<ParticipantContextConfiguration> encryptEntries(ParticipantContextConfiguration config) {

        Result<List<Map.Entry<String, String>>> result = config.getPrivateEntries()
                .entrySet()
                .stream()
                .map(this::encryptEntryMap)
                .collect(Result.collector());

        if (result.succeeded()) {
            var encryptedConfig = config.toBuilder()
                    .privateEntries(result.getContent().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .build();
            return Result.success(encryptedConfig);
        } else {
            return Result.failure("Failed to encrypt entries: " + result.getFailureDetail());

        }

    }

    private Result<Map.Entry<String, String>> encryptEntryMap(Map.Entry<String, String> entry) {
        return encryptionRegistry.encrypt(encryptionAlgorithm, entry.getValue())
                .map(encrypted -> Map.entry(entry.getKey(), encrypted));
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
