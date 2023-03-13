/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.model.Catalog;
import org.eclipse.edc.jsonld.model.DataService;
import org.eclipse.edc.jsonld.model.Dataset;
import org.eclipse.edc.jsonld.model.Distribution;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeId;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeType;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;

/**
 *
 */
public class ToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {
    private static final String DCAT_CATALOG = "http://www.w3.org/ns/dcat/Catalog";
    private static final String DCAT_DATASET = "http://www.w3.org/ns/dcat/dataset";
    private static final String DCAT_DISTRIBUTION = "http://www.w3.org/ns/dcat/distribution";
    private static final String DCAT_DATA_SERVICE = "http://www.w3.org/ns/dcat/DataService";

    public ToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        var type = nodeType(object, context);
        if (DCAT_CATALOG.equals(type)) {
            var builder = Catalog.Builder.newInstance();

            builder.id(nodeId(object));

            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));

            return builder.build();
        }
        return null;
    }

    private void transformProperties(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (DCAT_DATASET.equals(key)) {
            transformDatasets(value, builder, context);
        } else if (DCAT_DISTRIBUTION.equals(key)) {
            transformDistributions(value, builder, context);
        } else if (DCAT_DATA_SERVICE.equals(key)) {
            transformDataServices(value, builder, context);
        } else {
            transformGenericProperty(key, value, builder, context);
        }
    }

    private void transformDatasets(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var datasets = jsonArray.stream().map(entry -> context.transform(entry, Dataset.class)).collect(toList());
            builder.datasets(datasets);
        } else if (value instanceof JsonObject) {
            var dataset = context.transform(value, Dataset.class);
            builder.dataset(dataset);
        } else {
            context.reportProblem("Invalid dataset property");
        }
    }

    private void transformDataServices(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var dataServices = jsonArray.stream().map(entry -> context.transform(entry, DataService.class)).collect(toList());
            // builder.dataServices(dataServices);
        } else if (value instanceof JsonObject) {
            var dataService = context.transform(value, DataService.class);
            // builder.dataService(dataService);
        } else {
            context.reportProblem("Invalid DataService property");
        }
    }

    private void transformDistributions(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var distributions = jsonArray.stream().map(entry -> context.transform(entry, Distribution.class)).collect(toList());
            // builder.distributions(distributions);
        } else if (value instanceof JsonObject) {
            var distribution = context.transform(value, Distribution.class);
            // builder.distribution(distribution);
        } else {
            context.reportProblem("Invalid DataService property");
        }
    }

    private void transformGenericProperty(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            if (jsonArray.isEmpty()) {
                builder.property(key, List.of());
            } else if (jsonArray.size() == 1) {
                // unwrap array
                var result = context.transform(jsonArray.get(0), Object.class);
                builder.property(key, result);
            } else {
                var result = jsonArray.stream().map(prop -> context.transform(prop, Object.class)).collect(toList());
                builder.property(key, result);
            }
        } else {
            var result = context.transform(value, Object.class);
            builder.property(key, result);
        }
    }

}
