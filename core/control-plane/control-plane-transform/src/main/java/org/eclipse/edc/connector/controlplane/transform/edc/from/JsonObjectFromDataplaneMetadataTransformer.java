/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

public class JsonObjectFromDataplaneMetadataTransformer extends AbstractJsonLdTransformer<DataplaneMetadata, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataplaneMetadataTransformer(JsonBuilderFactory jsonFactory) {
        super(DataplaneMetadata.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataplaneMetadata dataplaneMetadata, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, EDC_DATAPLANE_METADATA_TYPE);

        var labels = dataplaneMetadata.getLabels();
        if (!labels.isEmpty()) {
            var arrayBuilder = jsonFactory.createArrayBuilder();
            labels.forEach(arrayBuilder::add);
            builder.add(DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS, arrayBuilder);
        }

        if (dataplaneMetadata.getProperties() != null && !dataplaneMetadata.getProperties().isEmpty()) {
            builder.add(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES, jsonFactory.createObjectBuilder()
                    .add(VALUE, jsonFactory.createObjectBuilder(dataplaneMetadata.getProperties()))
                    .add(TYPE, JSON)
                    .build());
        }

        return builder.build();
    }
}
