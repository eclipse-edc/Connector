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

package org.eclipse.edc.validator.registration;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorFactory;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wires {@link SchemaValidatorRegistration}s into the shared {@link JsonObjectValidatorRegistry} at runtime.
 * <p>
 * Because the registry supports registration but not de-registration, exactly one <em>dynamic</em> validator is
 * registered per {@code version:validatedType} key. That validator consults the store on every invocation: it
 * validates the input against the schema of every active binding for the key (composing the results) and returns
 * success when no binding exists. Consequently creating, updating and deleting registrations takes effect without
 * touching the registry.
 */
public class DynamicSchemaValidatorRegistrar {

    private final JsonObjectValidatorRegistry registry;
    private final SchemaValidatorFactory factory;
    private final SchemaValidatorRegistrationStore store;
    private final Set<String> registeredKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, Validator<JsonObject>> validatorCache = new ConcurrentHashMap<>();

    public DynamicSchemaValidatorRegistrar(JsonObjectValidatorRegistry registry, SchemaValidatorFactory factory,
                                           SchemaValidatorRegistrationStore store) {
        this.registry = registry;
        this.factory = factory;
        this.store = store;
    }

    /**
     * Ensures a dynamic validator is registered for the given {@code version}/{@code validatedType}. Idempotent:
     * the validator is registered at most once per key.
     */
    public void ensureRegistered(String version, String validatedType) {
        var key = version + ":" + validatedType;
        if (registeredKeys.add(key)) {
            registry.register(key, dynamicValidatorFor(version, validatedType));
        }
    }

    /**
     * Drops the cached validator built for the given schema {@code $id}, forcing it to be rebuilt on next use
     * (e.g. after a registration was updated to point at a different schema).
     */
    public void evict(String schemaId) {
        validatorCache.remove(schemaId);
    }

    private Validator<JsonObject> dynamicValidatorFor(String version, String validatedType) {
        return input -> {
            var bindings = store.findByVersionAndValidatedType(version, validatedType);
            if (bindings.isEmpty()) {
                return ValidationResult.success();
            }
            return bindings.stream()
                    .map(binding -> applyBinding(binding, input))
                    .reduce(ValidationResult.success(), ValidationResult::merge);
        };
    }

    private ValidationResult applyBinding(SchemaValidatorRegistration binding, JsonObject input) {
        if (!binding.getProfiles().isEmpty()) {
            var profile = extractProfile(input);
            // Skip validation if the input carries a profile that is not associated with the binding; this allows
            // multiple bindings for the same type to be discriminated by profile.
            if (profile == null || !binding.getProfiles().contains(profile)) {
                return ValidationResult.success();
            }
        }
        return validatorCache.computeIfAbsent(binding.getSchema(), factory::validatorFor).validate(input);
    }

    private String extractProfile(JsonObject input) {
        if (input.get("policy") instanceof JsonObject policy && policy.get("profile") instanceof JsonString profile) {
            return profile.getString();
        }
        return null;
    }
}
