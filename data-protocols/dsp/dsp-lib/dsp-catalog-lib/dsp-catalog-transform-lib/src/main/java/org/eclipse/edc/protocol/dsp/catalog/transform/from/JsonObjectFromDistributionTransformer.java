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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

/**
 * Converts from a {@link Distribution} to a DCAT distribution as a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromDistributionTransformer extends AbstractJsonLdTransformer<Distribution, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDistributionTransformer(JsonBuilderFactory jsonFactory) {
        super(Distribution.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Distribution distribution, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, DCAT_DISTRIBUTION_TYPE)
                .add(DCT_FORMAT_ATTRIBUTE, jsonFactory.createObjectBuilder()
                    .add(ID, distribution.getFormat())
                    .build())
                .add(DCAT_ACCESS_SERVICE_ATTRIBUTE, context.transform(distribution.getDataService(), JsonObject.class))
                .build();
    }

}
