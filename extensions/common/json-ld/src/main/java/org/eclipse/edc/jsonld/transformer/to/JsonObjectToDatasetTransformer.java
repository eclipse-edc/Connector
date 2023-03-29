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
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;

public class JsonObjectToDatasetTransformer extends AbstractJsonLdTransformer<JsonObject, Dataset> {

    private static final String DCAT_DATASET_TYPE = DCAT_SCHEMA + "Dataset";
    private static final String DCAT_DISTRIBUTION_PROPERTY = DCAT_SCHEMA + "distribution";
    private static final String DCAT_POLICY_PROPERTY = DCAT_SCHEMA + "hasPolicy";

    public JsonObjectToDatasetTransformer() {
        super(JsonObject.class, Dataset.class);
    }

    @Override
    public @Nullable Dataset transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var type = nodeType(object, context);
        if (DCAT_DATASET_TYPE.equals(type)) {
            var builder = Dataset.Builder.newInstance();

            builder.id(nodeId(object));
            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));

            return builder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to Dataset", type));
            return null;
        }
    }

    private void transformProperties(String key, JsonValue value, Dataset.Builder builder, TransformerContext context) {
        if (DCAT_POLICY_PROPERTY.equals(key)) {
            transformPolicies(value, builder, context);
        } else if (DCAT_DISTRIBUTION_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Distribution.class, builder::distribution, builder::distributions, context);
        } else {
            builder.property(key, transformGenericProperty(value, context));
        }
    }

    private void transformPolicies(JsonValue value, Dataset.Builder builder, TransformerContext context) {
        if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            var id = nodeId(object);
            var policy = context.transform(object, Policy.class);
            builder.offer(id, policy);
        } else if (value instanceof JsonArray) {
            var array = (JsonArray) value;
            array.stream().forEach(entry -> transformPolicies(entry, builder, context));
        } else {
            context.reportProblem("Invalid hasPolicy property");
        }
    }
}
