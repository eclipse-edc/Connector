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

package org.eclipse.edc.protocol.dsp.transform.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.protocol.dsp.transform.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transform.transformer.JsonLdKeywords.ID;
import static org.eclipse.edc.protocol.dsp.transform.transformer.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_SCHEMA;

public class JsonObjectFromDatasetTransformer extends AbstractJsonLdTransformer<Dataset, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromDatasetTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Dataset.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Dataset dataset, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(ID, dataset.getId());
        objectBuilder.add(TYPE, DCAT_SCHEMA + "Dataset");

        var policies = transformOffers(dataset, context);
        objectBuilder.add(DCAT_SCHEMA + "hasPolicy", policies);

        var distributions = dataset.getDistributions().stream()
                .map(distribution -> context.transform(distribution, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_SCHEMA + "distribution", distributions);

        // transform properties, which are generic JSON values.
        dataset.getProperties().forEach((k, v) -> objectBuilder.add(k, mapper.convertValue(v, JsonValue.class)));

        return objectBuilder.build();
    }

    private JsonValue transformOffers(Dataset dataset, TransformerContext context) {
        var builder = jsonFactory.createArrayBuilder();
        for (var entry : dataset.getOffers().entrySet()) {
            var policy = context.transform(entry.getValue(), JsonObject.class);

            var policyBuilder = jsonFactory.createObjectBuilder(policy);
            policyBuilder.add(ID, Json.createValue(entry.getKey()));
            builder.add(policyBuilder.build());

        }
        return builder.build();
    }
}
