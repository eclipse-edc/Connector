/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;

/**
 * Converts from a DCAT catalog as a {@link JsonObject} in JSON-LD expanded form to a {@link Catalog}.
 */
public class JsonObjectToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {

    public JsonObjectToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Catalog.Builder.newInstance();

        builder.id(nodeId(object));
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));

        return builderResult(builder::build, context);
    }

    private @Nullable Dataset transformDataset(JsonValue datasetJsonObj, TransformerContext context) {
        return transformObject(datasetJsonObj, Dataset.class, context);
    }

    private void transformProperties(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (DCAT_DATASET_ATTRIBUTE.equalsIgnoreCase(key)) {
            if (value.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                value.asJsonArray().stream()
                        .map(jv -> transformDataset(jv, context))
                        .forEach(builder::dataset);
            } else {
                builder.dataset(transformDataset(value, context));
            }
        } else if (DCAT_CATALOG_ATTRIBUTE.equalsIgnoreCase(key)) {
            transformArrayOrObject(value, Catalog.class, builder::dataset, context);
        } else if (DCAT_DATA_SERVICE_ATTRIBUTE.equalsIgnoreCase(key)) {
            transformArrayOrObject(value, DataService.class, builder::dataService, context);
        } else if (DSP_NAMESPACE_V_2025_1.toIri(DSPACE_PROPERTY_PARTICIPANT_ID_TERM).equalsIgnoreCase(key)) {
            builder.participantId(transformString(value, context));
        } else if (DCAT_DISTRIBUTION_TYPE.equalsIgnoreCase(key)) {
            transformArrayOrObject(value, Distribution.class, builder::distribution, context);
        } else {
            builder.property(key, transformGenericProperty(value, context));
        }
    }
}
