/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_TYPE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_INCLUDED_IN_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REFINEMENT_ATTRIBUTE;

/**
 * Converts from an ODRL action as a {@link JsonObject} in JSON-LD expanded form to an {@link Action}.
 */
public class JsonObjectToActionTransformer extends AbstractJsonLdTransformer<JsonObject, Action> {

    public JsonObjectToActionTransformer() {
        super(JsonObject.class, Action.class);
    }

    @Override
    public @Nullable Action transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Action.Builder.newInstance();
        var type = Optional.ofNullable(nodeId(object)).orElseGet(() -> nodeValue(object));
        builder.type(type);
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue value, Action.Builder builder, TransformerContext context) {
        // we read the type for backward compatibility
        if (ODRL_ACTION_TYPE_ATTRIBUTE.equals(key)) {
            transformString(value, builder::type, context);
        } else if (ODRL_ACTION_ATTRIBUTE.equals(key)) {
            transformString(value, builder::type, context);
        } else if (ODRL_INCLUDED_IN_ATTRIBUTE.equals(key)) {
            transformString(value, builder::includedIn, context);
        } else if (ODRL_REFINEMENT_ATTRIBUTE.equals(key)) {
            transformArrayOrObject(value, Constraint.class, builder::constraint, context);
        }
    }
}
