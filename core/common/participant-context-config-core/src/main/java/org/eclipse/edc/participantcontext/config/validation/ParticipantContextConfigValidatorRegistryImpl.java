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

package org.eclipse.edc.participantcontext.config.validation;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigView;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantContextConfigValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.edc.validator.spi.Violation.violation;

public class ParticipantContextConfigValidatorRegistryImpl implements ParticipantContextConfigValidatorRegistry {

    private final Map<String, ParticipantConfigEntry> entries = new ConcurrentHashMap<>();
    private final boolean strict;

    /**
     * Creates the registry.
     *
     * @param strict whether keys that are not registered are rejected.
     */
    public ParticipantContextConfigValidatorRegistryImpl(boolean strict) {
        this.strict = strict;
    }

    @Override
    public void register(ParticipantConfigEntry entry) {
        entries.put(entry.getKey(), entry);
    }

    @Override
    public ValidationResult validateWrite(ParticipantContextConfiguration write, ParticipantConfigView effective) {
        var violations = new ArrayList<Violation>();
        write.getEntries().forEach((key, value) -> {
            if (value != null) {
                violations.addAll(validateEntry(key, value, false));
            }
        });
        write.getPrivateEntries().forEach((key, value) -> {
            if (value != null) {
                violations.addAll(validateEntry(key, value, true));
            }
        });
        violations.addAll(missingRequiredKeys(effective));
        return toResult(violations);
    }

    @Override
    public ValidationResult validate(ParticipantConfigView configuration) {
        var violations = new ArrayList<Violation>();
        configuration.keys().forEach(key -> {
            if (configuration.isPrivate(key)) {
                // stored private values are encrypted, only their placement can be checked
                violations.addAll(validatePlacement(key, true));
            } else {
                configuration.getEntry(key).ifPresent(value -> violations.addAll(validateEntry(key, value, false)));
            }
        });
        violations.addAll(missingRequiredKeys(configuration));
        return toResult(violations);
    }

    private List<Violation> validateEntry(String key, String value, boolean isPrivate) {
        var placement = validatePlacement(key, isPrivate);
        if (!placement.isEmpty()) {
            return placement;
        }
        return Optional.ofNullable(entries.get(key))
                .map(entry -> entry.validate(value))
                .filter(ValidationResult::failed)
                .map(result -> result.getFailure().getViolations().stream()
                        // never echo the value of a private entry
                        .map(it -> violation("'%s' %s".formatted(key, it.message()), key, isPrivate ? null : value))
                        .toList())
                .orElse(List.of());
    }

    private List<Violation> validatePlacement(String key, boolean isPrivate) {
        var entry = entries.get(key);
        if (entry == null) {
            return strict ? List.of(violation("'%s' is not a known configuration key".formatted(key), key)) : List.of();
        }
        if (entry.isSensitive() && !isPrivate) {
            return List.of(violation("'%s' is sensitive and must be set as a private entry".formatted(key), key));
        }
        if (!entry.isSensitive() && isPrivate) {
            return List.of(violation("'%s' is not sensitive and must be set as a public entry".formatted(key), key));
        }
        return List.of();
    }

    private List<Violation> missingRequiredKeys(ParticipantConfigView configuration) {
        return entries.values().stream()
                .filter(entry -> !configuration.hasKey(entry.getKey()) && entry.isRequired(configuration))
                .map(entry -> violation("'%s' is required".formatted(entry.getKey()), entry.getKey()))
                .toList();
    }

    private ValidationResult toResult(List<Violation> violations) {
        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
    }
}
