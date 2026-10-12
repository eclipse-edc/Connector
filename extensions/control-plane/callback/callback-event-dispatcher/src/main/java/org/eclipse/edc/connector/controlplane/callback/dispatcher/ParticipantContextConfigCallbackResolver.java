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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.ParticipantCallbackResolver;
import org.eclipse.edc.controlplane.CallbackAddress;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Resolves the callbacks of a participant context from its configuration entry {@value #CALLBACKS_CONFIG_KEY}, which holds a
 * JSON array of {@link CallbackAddress}.
 */
public class ParticipantContextConfigCallbackResolver implements ParticipantCallbackResolver {

    public static final String CALLBACKS_CONFIG_KEY = "edc.callbacks";

    private final ParticipantContextConfig participantContextConfig;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final Monitor monitor;

    public ParticipantContextConfigCallbackResolver(ParticipantContextConfig participantContextConfig, Supplier<ObjectMapper> mapperSupplier, Monitor monitor) {
        this.participantContextConfig = participantContextConfig;
        this.mapperSupplier = mapperSupplier;
        this.monitor = monitor;
    }

    /**
     * Validator for the {@value #CALLBACKS_CONFIG_KEY} configuration value.
     *
     * @param mapperSupplier supplies the mapper used to parse the value.
     * @return the validator.
     */
    public static Validator<String> callbacksValidator(Supplier<ObjectMapper> mapperSupplier) {
        return value -> {
            if (value.isBlank()) {
                return ValidationResult.success();
            }
            try {
                var callbacks = mapperSupplier.get().readValue(value, new TypeReference<List<CallbackAddress>>() { });
                if (callbacks.stream().anyMatch(Objects::isNull)) {
                    return ValidationResult.failure(violation("must not contain null callbacks", null));
                }
                return ValidationResult.success();
            } catch (JacksonException e) {
                return ValidationResult.failure(violation("must be a JSON array of callback addresses: " + e.getOriginalMessage(), null));
            }
        };
    }

    @Override
    public List<CallbackAddress> resolve(String participantContextId, String eventName) {
        var callbacks = participantContextConfig.getString(participantContextId, CALLBACKS_CONFIG_KEY, "");
        if (callbacks.isBlank()) {
            return List.of();
        }

        try {
            return mapperSupplier.get().readValue(callbacks, new TypeReference<List<CallbackAddress>>() { })
                    .stream()
                    .filter(callback -> callback.getEvents().stream().anyMatch(eventName::startsWith))
                    .toList();
        } catch (JacksonException e) {
            // an invalid configuration must not break the event dispatching, which could happen in a transactional context
            monitor.warning("Invalid %s configuration for participant context %s: %s".formatted(CALLBACKS_CONFIG_KEY, participantContextId, e.getMessage()));
            return List.of();
        }
    }
}
