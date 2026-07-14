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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_PROFILES_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_IRI;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_VERSION_IRI;

/**
 * Transforms the (expanded) request body of the schema validator registration API into a
 * {@link SchemaValidatorRegistration}.
 */
public class JsonObjectToSchemaValidatorRegistrationTransformer extends AbstractJsonLdTransformer<JsonObject, SchemaValidatorRegistration> {

    public JsonObjectToSchemaValidatorRegistrationTransformer() {
        super(JsonObject.class, SchemaValidatorRegistration.class);
    }

    @Override
    public @Nullable SchemaValidatorRegistration transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var version = transformString(object.get(SCHEMA_VALIDATOR_REGISTRATION_VERSION_IRI), context);
        if (version == null) {
            context.reportProblem("Property 'version' is mandatory");
            return null;
        }
        var validatedType = transformString(object.get(SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_IRI), context);
        if (validatedType == null) {
            context.reportProblem("Property 'validatedType' is mandatory");
            return null;
        }
        var schema = transformString(object.get(SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_IRI), context);
        if (schema == null) {
            context.reportProblem("Property 'schema' is mandatory");
            return null;
        }

        var builder = SchemaValidatorRegistration.Builder.newInstance()
                .version(version)
                .validatedType(validatedType)
                .schema(schema);

        var id = nodeId(object);
        if (id != null) {
            builder.id(id);
        }

        var profiles = new ArrayList<String>();
        var profilesValue = object.get(SCHEMA_VALIDATOR_REGISTRATION_PROFILES_IRI);
        if (profilesValue != null) {
            visitArray(profilesValue, value -> transformString(value, profiles::add, context), context);
        }
        builder.profiles(profiles);

        return builder.build();
    }
}
