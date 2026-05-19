/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.connector.profile;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ParticipantProfileServiceImpl implements ParticipantProfileService {

    private final ParticipantContextConfigStore participantContextConfigStore;
    private final DataspaceProfileContextRegistry profileRegistry;
    private final TransactionContext transactionContext;
    private final Boolean dspEnableAllProfiles;

    public ParticipantProfileServiceImpl(ParticipantContextConfigStore participantContextConfigStore,
                                         DataspaceProfileContextRegistry profileRegistry, TransactionContext transactionContext, Boolean dspEnableAllProfiles) {
        this.participantContextConfigStore = participantContextConfigStore;
        this.profileRegistry = profileRegistry;
        this.transactionContext = transactionContext;
        this.dspEnableAllProfiles = dspEnableAllProfiles;
    }

    private Set<String> parse(String csv) {
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<DataspaceProfileContext> resolveAll(String participantContextId) {
        if (dspEnableAllProfiles) {
            return profileRegistry.getProfiles();
        }
        return transactionContext.execute(() -> {
            var result = readRaw(participantContextId)
                    .map(this::parse)
                    .map(profiles -> profiles.stream()
                            .map(profileRegistry::getProfile)
                            .filter(Objects::nonNull)
                            .toList());

            if (result.failed()) {
                return List.of();
            }
            return result.getContent();
        });

    }

    @Override
    public DataspaceProfileContext resolve(String participantContextId, String profileId) {

        if (dspEnableAllProfiles) {
            return profileRegistry.getProfile(profileId);
        }

        return transactionContext.execute(() -> {
            var result = readRaw(participantContextId)
                    .map(this::parse)
                    .map(configured -> configured.contains(profileId));

            if (result.failed() || !result.getContent()) {
                return null;
            }

            return profileRegistry.getProfile(profileId);
        });

    }

    private ServiceResult<String> readRaw(String participantContextId) {
        return Optional.ofNullable(getParticipantContextConfig(participantContextId))
                .map(cfg -> ServiceResult.success(cfg.getEntries().getOrDefault(PROFILES_CONFIG_KEY, "")))
                .orElseGet(() -> ServiceResult.notFound("No participant context config found for id: " + participantContextId));
    }

    private ParticipantContextConfiguration getParticipantContextConfig(String participantContextId) {
        return participantContextConfigStore.get(participantContextId);
    }

    @Override
    public ServiceResult<Void> associateProfiles(String participantContextId, List<String> profiles) {

        var result = profiles.stream().map(this::validateProfile)
                .reduce(ValidationResult.success(), ValidationResult::merge);

        if (result.succeeded()) {
            return transactionContext.execute(() -> {
                var storedConfig = participantContextConfigStore.get(participantContextId);
                // automatically create config if it does not exist, otherwise update existing config
                var config = Optional.ofNullable(storedConfig)
                        .orElseGet(() -> ParticipantContextConfiguration.Builder.newInstance()
                                .participantContextId(participantContextId)
                                .build());
                config.getEntries().put(PROFILES_CONFIG_KEY, String.join(",", profiles));
                participantContextConfigStore.save(config);
                return ServiceResult.success();
            });

        } else {
            return ServiceResult.badRequest(result.getFailureMessages());
        }
    }

    private @NotNull ValidationResult validateProfile(String p) {
        if (profileRegistry.getProfile(p) == null) {
            return ValidationResult.failure(Violation.violation("Profile " + p + " does not exist", null));
        }
        return ValidationResult.success();
    }
}
