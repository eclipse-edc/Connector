/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.core.transform.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;

/**
 * Converts from an ODRL policy as a {@link JsonObject} in JSON-LD expanded form to a {@link Policy}.
 */
public class JsonObjectToPolicyTransformer extends AbstractJsonLdTransformer<JsonObject, Policy> {

    public JsonObjectToPolicyTransformer() {
        super(JsonObject.class, Policy.class);
    }

    @Override
    public @Nullable Policy transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Policy.Builder.newInstance();

        visitProperties(object, key -> {
            switch (key) {
                case ODRL_PERMISSION_ATTRIBUTE:
                    return v -> builder.permissions(transformArray(v, Permission.class, context));
                case ODRL_PROHIBITION_ATTRIBUTE:
                    return v -> builder.prohibitions(transformArray(v, Prohibition.class, context));
                case ODRL_OBLIGATION_ATTRIBUTE:
                    return v -> builder.duties(transformArray(v, Duty.class, context));
                case ODRL_TARGET_ATTRIBUTE:
                    return v -> builder.target(transformString(v, context));
                default:
                    return v -> builder.extensibleProperty(key, transformGenericProperty(v, context));
            }
        });

        return builderResult(builder::build, context);
    }

}
