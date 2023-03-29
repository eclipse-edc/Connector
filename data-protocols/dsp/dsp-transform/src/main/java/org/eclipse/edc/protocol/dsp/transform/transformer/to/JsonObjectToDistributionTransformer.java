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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.protocol.dsp.transform.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

public class JsonObjectToDistributionTransformer extends AbstractJsonLdTransformer<JsonObject, Distribution> {

    public JsonObjectToDistributionTransformer() {
        super(JsonObject.class, Distribution.class);
    }

    @Override
    public @Nullable Distribution transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var type = nodeType(object, context);
        if (DCAT_DISTRIBUTION_TYPE.equals(type)) {
            var builder = Distribution.Builder.newInstance();
            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
            return builder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to Distribution", type));
            return null;
        }
    }

    private void transformProperties(String key, JsonValue value, Distribution.Builder builder, TransformerContext context) {
        if (DCAT_ACCESS_SERVICE_ATTRIBUTE.equals(key)) {
            transformString(value, builder::dataServiceId, context);
        } else if (DCT_FORMAT_ATTRIBUTE.equals(key)) {
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
