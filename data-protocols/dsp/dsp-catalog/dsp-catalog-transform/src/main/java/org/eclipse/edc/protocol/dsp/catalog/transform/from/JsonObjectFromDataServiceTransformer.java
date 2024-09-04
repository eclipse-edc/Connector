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

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ENDPOINT_DESCRIPTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ENDPOINT_URL_ATTRIBUTE;

/**
 * Converts from a {@link DataService} to a DCAT data service as a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromDataServiceTransformer extends AbstractJsonLdTransformer<DataService, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataServiceTransformer(JsonBuilderFactory jsonFactory) {
        super(DataService.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataService dataService, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder()
                .add(ID, dataService.getId())
                .add(TYPE, DCAT_DATA_SERVICE_TYPE);

        addIfNotNull(dataService.getEndpointDescription(), DCAT_ENDPOINT_DESCRIPTION_ATTRIBUTE, objectBuilder);
        addIfNotNull(dataService.getEndpointUrl(), DCAT_ENDPOINT_URL_ATTRIBUTE, objectBuilder);

        return objectBuilder.build();
    }

}
