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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_PROFILES_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_TYPE_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_UPDATED_AT_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_VERSION_IRI;

/**
 * Renders a {@link SchemaValidatorRegistration} as an (expanded) JSON-LD object.
 */
public class JsonObjectFromSchemaValidatorRegistrationTransformer extends AbstractJsonLdTransformer<SchemaValidatorRegistration, JsonObject> {

    private final JsonBuilderFactory factory;

    public JsonObjectFromSchemaValidatorRegistrationTransformer(JsonBuilderFactory factory) {
        super(SchemaValidatorRegistration.class, JsonObject.class);
        this.factory = factory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull SchemaValidatorRegistration registration, @NotNull TransformerContext context) {
        var profiles = factory.createArrayBuilder();
        registration.getProfiles().forEach(profiles::add);

        return factory.createObjectBuilder()
                .add(ID, registration.getId())
                .add(TYPE, SCHEMA_VALIDATOR_REGISTRATION_TYPE_IRI)
                .add(SCHEMA_VALIDATOR_REGISTRATION_VERSION_IRI, registration.getVersion())
                .add(SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_IRI, registration.getValidatedType())
                .add(SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_IRI, registration.getSchema())
                .add(SCHEMA_VALIDATOR_REGISTRATION_PROFILES_IRI, profiles)
                .add(SCHEMA_VALIDATOR_REGISTRATION_UPDATED_AT_IRI, registration.getUpdatedAt())
                .build();
    }
}
