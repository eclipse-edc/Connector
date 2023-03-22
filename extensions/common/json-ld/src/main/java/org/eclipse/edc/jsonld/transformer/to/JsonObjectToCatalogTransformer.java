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
 *       Fraunhofer Institute for Software and Systems Engineering - implement methods
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeId;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeType;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformArrayOrObject;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformGenericProperty;

public class JsonObjectToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {

    private static final String DCAT_CATALOG_TYPE = DCAT_SCHEMA + "Catalog";
    private static final String DCAT_DATASET_PROPERTY = DCAT_SCHEMA + "dataset";
    private static final String DCAT_DATA_SERVICE_PROPERTY = DCAT_SCHEMA + "DataService";

    public JsonObjectToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
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
            transformArrayOrObject(value, Dataset.class, builder::dataset, builder::datasets, context);
        } else if (DCAT_DATA_SERVICE_PROPERTY.equals(key)) {
            transformArrayOrObject(value, DataService.class, builder::dataService, builder::dataServices, context);
        } else {
            builder.property(key, transformGenericProperty(value, context));
        }
    }
}
