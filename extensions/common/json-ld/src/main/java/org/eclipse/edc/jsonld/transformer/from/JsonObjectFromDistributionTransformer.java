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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_SCHEMA;

public class JsonObjectFromDistributionTransformer extends AbstractJsonLdTransformer<Distribution, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromDistributionTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Distribution.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable Distribution distribution, @NotNull TransformerContext context) {
        if (distribution == null) {
            return null;
        }

        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(TYPE, DCAT_SCHEMA + "Distribution");

        objectBuilder.add(DCT_SCHEMA + "format", jsonFactory.createObjectBuilder()
                .add("@id", distribution.getFormat())
                .build());

        objectBuilder.add(DCAT_SCHEMA + "accessService", distribution.getDataService().getId());

        return objectBuilder.build();
    }

}
