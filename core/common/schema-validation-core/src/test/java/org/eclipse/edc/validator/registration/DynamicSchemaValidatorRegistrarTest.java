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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorFactory;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicSchemaValidatorRegistrarTest {

    private final JsonObjectValidatorRegistry registry = mock();
    private final SchemaValidatorFactory factory = mock();
    private final SchemaValidatorRegistrationStore store = mock();
    private final DynamicSchemaValidatorRegistrar registrar = new DynamicSchemaValidatorRegistrar(registry, factory, store);

    @SuppressWarnings("unchecked")
    private Validator<JsonObject> captureRegistered(String key) {
        var captor = ArgumentCaptor.forClass(Validator.class);
        verify(registry).register(eq(key), captor.capture());
        return captor.getValue();
    }

    private SchemaValidatorRegistration binding(String schema, List<String> profiles) {
        return SchemaValidatorRegistration.Builder.newInstance()
                .version("v5").validatedType("Asset").schema(schema).profiles(profiles).build();
    }

    @Test
    void ensureRegistered_registersOncePerKey() {
        registrar.ensureRegistered("v5", "Asset");
        registrar.ensureRegistered("v5", "Asset");
        registrar.ensureRegistered("v5", "Asset");

        verify(registry, times(1)).register(eq("v5:Asset"), any());
    }

    @Test
    void dynamicValidator_returnsSuccess_whenNoBindings() {
        when(store.findByVersionAndValidatedType("v5", "Asset")).thenReturn(List.of());
        registrar.ensureRegistered("v5", "Asset");
        var validator = captureRegistered("v5:Asset");

        var result = validator.validate(Json.createObjectBuilder().build());

        assertThat(result.succeeded()).isTrue();
        verify(factory, never()).validatorFor(any());
    }

    @Test
    void dynamicValidator_validatesAgainstBindingSchema() {
        when(store.findByVersionAndValidatedType("v5", "Asset")).thenReturn(List.of(binding("s1", List.of())));
        when(factory.validatorFor("s1")).thenReturn(input -> ValidationResult.failure(Violation.violation("bad", "/x")));
        registrar.ensureRegistered("v5", "Asset");
        var validator = captureRegistered("v5:Asset");

        var result = validator.validate(Json.createObjectBuilder().build());

        assertThat(result.failed()).isTrue();
    }

    @Test
    void dynamicValidator_memoizesValidatorPerSchema() {
        when(store.findByVersionAndValidatedType("v5", "Asset")).thenReturn(List.of(binding("s1", List.of())));
        when(factory.validatorFor("s1")).thenReturn(input -> ValidationResult.success());
        registrar.ensureRegistered("v5", "Asset");
        var validator = captureRegistered("v5:Asset");

        validator.validate(Json.createObjectBuilder().build());
        validator.validate(Json.createObjectBuilder().build());

        verify(factory, times(1)).validatorFor("s1");
    }

    @Test
    void dynamicValidator_skipsValidation_whenProfileDoesNotMatch() {
        when(store.findByVersionAndValidatedType("v5", "Asset")).thenReturn(List.of(binding("s1", List.of("custom"))));
        registrar.ensureRegistered("v5", "Asset");
        var validator = captureRegistered("v5:Asset");

        // no policy.profile in the input -> profile-filtered binding is skipped
        var result = validator.validate(Json.createObjectBuilder().build());

        assertThat(result.succeeded()).isTrue();
        verify(factory, never()).validatorFor(any());
    }

    @Test
    void dynamicValidator_validates_whenProfileMatches() {
        when(store.findByVersionAndValidatedType("v5", "Asset")).thenReturn(List.of(binding("s1", List.of("custom"))));
        when(factory.validatorFor("s1")).thenReturn(input -> ValidationResult.failure(Violation.violation("bad", "/x")));
        registrar.ensureRegistered("v5", "Asset");
        var validator = captureRegistered("v5:Asset");

        var input = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder().add("profile", "custom"))
                .build();

        var result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }
}
