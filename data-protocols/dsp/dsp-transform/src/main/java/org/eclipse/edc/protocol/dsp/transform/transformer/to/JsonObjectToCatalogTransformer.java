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
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.protocol.dsp.transform.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_SCHEMA;

public class JsonObjectToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {

    private static final String DCAT_CATALOG_TYPE = DCAT_SCHEMA + "Catalog";
    private static final String DCAT_DATASET_PROPERTY = DCAT_SCHEMA + "dataset";
    private static final String DCAT_DATA_SERVICE_PROPERTY = DCAT_SCHEMA + "DataService";

    public JsonObjectToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var type = nodeType(object, context);
        if (DCAT_CATALOG_TYPE.equals(type)) {
            var builder = Catalog.Builder.newInstance();

            builder.id(nodeId(object));
            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));

            return builder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to Catalog", type));
            return null;
        }
    }

    private void transformProperties(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (DCAT_DATASET_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Dataset.class, builder::dataset, context);
        } else if (DCAT_DATA_SERVICE_PROPERTY.equals(key)) {
            transformArrayOrObject(value, DataService.class, builder::dataService, context);
        } else {
            builder.property(key, transformGenericProperty(value, context));
        }
    }
}
