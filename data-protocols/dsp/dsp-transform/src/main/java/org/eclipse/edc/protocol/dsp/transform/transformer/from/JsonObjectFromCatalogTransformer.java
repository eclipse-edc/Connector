/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.transform.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;

/**
 * Converts from a {@link Catalog} to a DCAT catalog as a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromCatalogTransformer extends AbstractJsonLdTransformer<Catalog, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromCatalogTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Catalog.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Catalog catalog, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(ID, catalog.getId());
        objectBuilder.add(TYPE, DCAT_CATALOG_TYPE);

        var datasets = catalog.getDatasets().stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_DATASET_ATTRIBUTE, datasets);

        var dataServices = catalog.getDataServices().stream()
                .map(service -> context.transform(service, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_DATA_SERVICE_ATTRIBUTE, dataServices);
    
        transformProperties(catalog.getProperties(), objectBuilder, mapper, context);

        return objectBuilder.build();
    }
}
