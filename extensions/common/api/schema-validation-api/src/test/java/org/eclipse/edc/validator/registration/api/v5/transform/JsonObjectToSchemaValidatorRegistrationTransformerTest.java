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
import org.eclipse.edc.jsonld.test.TestJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM;

class JsonObjectToSchemaValidatorRegistrationTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TypeTransformerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeTransformerRegistryImpl();
        registry.register(new JsonObjectToSchemaValidatorRegistrationTransformer());
    }

    @Test
    void shouldTransformToEntity() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(ID, "https://example.com/ids/reg-1")
                .add(TYPE, SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM)
                .add("version", "v5")
                .add("validatedType", "Asset")
                .add("schema", "https://example.com/schema/asset.json")
                .add("profiles", jsonFactory.createArrayBuilder().add("custom").add("other"))
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), SchemaValidatorRegistration.class);

        assertThat(result).isSucceeded().satisfies(reg -> {
            assertThat(reg.getId()).isEqualTo("https://example.com/ids/reg-1");
            assertThat(reg.getVersion()).isEqualTo("v5");
            assertThat(reg.getValidatedType()).isEqualTo("Asset");
            assertThat(reg.getSchema()).isEqualTo("https://example.com/schema/asset.json");
            assertThat(reg.getProfiles()).containsExactlyInAnyOrder("custom", "other");
        });
    }

    @Test
    void shouldGenerateId_whenNotProvided() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM)
                .add("version", "v5")
                .add("validatedType", "Asset")
                .add("schema", "https://example.com/schema/asset.json")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), SchemaValidatorRegistration.class);

        assertThat(result).isSucceeded().satisfies(reg -> {
            assertThat(reg.getId()).isNotBlank();
            assertThat(reg.getProfiles()).isEmpty();
        });
    }

    @Test
    void shouldFail_whenSchemaMissing() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM)
                .add("version", "v5")
                .add("validatedType", "Asset")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), SchemaValidatorRegistration.class);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenValidatedTypeMissing() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM)
                .add("version", "v5")
                .add("schema", "https://example.com/schema/asset.json")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), SchemaValidatorRegistration.class);

        assertThat(result).isFailed();
    }
}
