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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformObject;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformString;

public class JsonObjectToActionTransformer extends AbstractJsonLdTransformer<JsonObject, Action> {
    
    private static final String ODRL_TYPE_PROPERTY = "type";
    private static final String ODRL_INCLUDED_IN_PROPERTY = "includedIn";
    private static final String ODRL_REFINEMENT_PROPERTY = "refinement";
    
    public JsonObjectToActionTransformer() {
        super(JsonObject.class, Action.class);
    }
    
    @Override
    public @Nullable Action transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        
        var builder = Action.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }
    
    private void transformProperties(String key, JsonValue value, Action.Builder builder, TransformerContext context) {
        if (ODRL_TYPE_PROPERTY.equals(key)) {
            transformString(value, builder::type, context);
        } else if (ODRL_INCLUDED_IN_PROPERTY.equals(key)) {
            transformString(value, builder::includedIn, context);
        } else if (ODRL_REFINEMENT_PROPERTY.equals(key)) {
            transformObject(value, Constraint.class, builder::constraint, context);
        }
    }
}
