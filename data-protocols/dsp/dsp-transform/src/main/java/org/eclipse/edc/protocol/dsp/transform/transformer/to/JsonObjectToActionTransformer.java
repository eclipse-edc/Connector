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

package org.eclipse.edc.protocol.dsp.transform.transformer.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.protocol.dsp.transform.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_ACTION_TYPE_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_INCLUDED_IN_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_REFINEMENT_ATTRIBUTE;


public class JsonObjectToActionTransformer extends AbstractJsonLdTransformer<JsonObject, Action> {
    
    public JsonObjectToActionTransformer() {
        super(JsonObject.class, Action.class);
    }
    
    @Override
    public @Nullable Action transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Action.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }
    
    private void transformProperties(String key, JsonValue value, Action.Builder builder, TransformerContext context) {
        if (ODRL_ACTION_TYPE_ATTRIBUTE.equals(key)) {
            transformString(value, builder::type, context);
        } else if (ODRL_INCLUDED_IN_ATTRIBUTE.equals(key)) {
            transformString(value, builder::includedIn, context);
        } else if (ODRL_REFINEMENT_ATTRIBUTE.equals(key)) {
            transformArrayOrObject(value, Constraint.class, builder::constraint, context);
        }
    }
}
