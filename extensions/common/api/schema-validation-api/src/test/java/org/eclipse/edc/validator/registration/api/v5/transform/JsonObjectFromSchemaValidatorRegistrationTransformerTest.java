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

package org.eclipse.edc.validator.registration.api.v5.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class JsonObjectFromSchemaValidatorRegistrationTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TypeTransformerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeTransformerRegistryImpl();
        registry.register(new JsonObjectFromSchemaValidatorRegistrationTransformer(jsonFactory));
        registry.register(new JsonObjectToSchemaValidatorRegistrationTransformer());
    }

    @Test
    void shouldTransformAndRoundTrip() {
        var registration = SchemaValidatorRegistration.Builder.newInstance()
                .id("reg-1")
                .version("v5")
                .validatedType("Asset")
                .schema("https://example.com/schema/asset.json")
                .profiles(List.of("custom"))
                .build();

        var json = registry.transform(registration, JsonObject.class);

        assertThat(json).isSucceeded();

        var roundTrip = registry.transform(json.getContent(), SchemaValidatorRegistration.class);

        assertThat(roundTrip).isSucceeded().satisfies(reg -> {
            assertThat(reg.getVersion()).isEqualTo("v5");
            assertThat(reg.getValidatedType()).isEqualTo("Asset");
            assertThat(reg.getSchema()).isEqualTo("https://example.com/schema/asset.json");
            assertThat(reg.getProfiles()).containsExactly("custom");
        });
    }
}
