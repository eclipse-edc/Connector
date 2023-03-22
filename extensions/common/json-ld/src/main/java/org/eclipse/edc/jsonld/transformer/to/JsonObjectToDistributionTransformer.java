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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeId;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeType;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformString;

public class JsonObjectToDistributionTransformer extends AbstractJsonLdTransformer<JsonObject, Distribution> {

    private static final String DCAT_DISTRIBUTION_TYPE = DCAT_SCHEMA + "Distribution";
    private static final String DCAT_ACCESS_SERVICE_PROPERTY = DCAT_SCHEMA + "accessService";
    private static final String DCT_FORMAT = DCT_SCHEMA + "format";

    public JsonObjectToDistributionTransformer() {
        super(JsonObject.class, Distribution.class);
    }

    @Override
    public @Nullable Distribution transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }

        var type = nodeType(object, context);
        if (DCAT_DISTRIBUTION_TYPE.equals(type)) {
            var builder = Distribution.Builder.newInstance();
            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
            return builder.build();
        }else {
            context.reportProblem(format("Cannot transform type %s to Distribution", type));
            return null;
        }
    }

    private void transformProperties(String key, JsonValue value, Distribution.Builder builder, TransformerContext context) {
        if (DCAT_ACCESS_SERVICE_PROPERTY.equals(key)) {
            transformString(value, builder::dataServiceId, context);
        } else if (DCT_FORMAT.equals(key)) {
            if (value instanceof JsonObject) {
                var format = nodeId((JsonObject) value);
                builder.format(format);
            } else if (value instanceof JsonArray) {
                var array = (JsonArray) value;
                var format = nodeId((JsonObject) array.get(0));
                builder.format(format);
            } else {
                context.reportProblem("Invalid format property.");
            }
        } else {
            context.reportProblem(format("Invalid property found for Distribution: %s", key));
        }
    }
}
