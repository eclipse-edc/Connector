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
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformArrayOrObject;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformGenericProperty;

public class JsonObjectToPolicyTransformer extends AbstractJsonLdTransformer<JsonObject, Policy> {
    
    private static final String ODRL_PERMISSION_PROPERTY = ODRL_SCHEMA + "permission";
    private static final String ODRL_PROHIBITION_PROPERTY = ODRL_SCHEMA + "prohibition";
    private static final String ODRL_OBLIGATION_PROPERTY = ODRL_SCHEMA + "obligation";
    
    public JsonObjectToPolicyTransformer() {
        super(JsonObject.class, Policy.class);
    }
    
    @Override
    public @Nullable Policy transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Policy.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }
    
    private void transformProperties(String key, JsonValue value, Policy.Builder builder, TransformerContext context) {
        if (ODRL_PERMISSION_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Permission.class, builder::permission, builder::permissions, context);
        } else if (ODRL_PROHIBITION_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Prohibition.class, builder::prohibition, builder::prohibitions, context);
        } else if (ODRL_OBLIGATION_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Duty.class, builder::duty, builder::duties, context);
        } else {
            builder.extensibleProperty(key, transformGenericProperty(value, context));
        }
    }
}
