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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;

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
    public @Nullable JsonObject transform(@Nullable Catalog catalog, @NotNull TransformerContext context) {
        if (catalog == null) {
            return null;
        }

        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(ID, catalog.getId());
        objectBuilder.add(TYPE, DCAT_SCHEMA + "Catalog");

        var datasets = catalog.getDatasets().stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_SCHEMA + "dataset", datasets);

        var dataServices = catalog.getDataServices().stream()
                .map(service -> context.transform(service, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_SCHEMA + "DataService", dataServices);

        // transform properties, which are generic JSON values.
        catalog.getProperties().forEach((k, v) -> objectBuilder.add(k, mapper.convertValue(v, JsonValue.class)));

        return objectBuilder.build();
    }
}
