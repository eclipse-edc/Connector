/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;

/**
 * Converts from a {@link Dataset} to a DCAT dataset as a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromDatasetTransformer extends AbstractJsonLdTransformer<Dataset, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDatasetTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(Dataset.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Dataset dataset, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(ID, dataset.getId());
        objectBuilder.add(TYPE, DCAT_DATASET_TYPE);

        var policies = transformOffers(dataset, context);
        objectBuilder.add(ODRL_POLICY_ATTRIBUTE, policies);

        var distributions = dataset.getDistributions().stream()
                .map(distribution -> context.transform(distribution, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
        objectBuilder.add(DCAT_DISTRIBUTION_ATTRIBUTE, distributions);

        transformProperties(dataset.getProperties(), objectBuilder, typeManager.getMapper(typeContext), context);

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
