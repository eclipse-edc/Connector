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

package org.eclipse.edc.connector.controlplane.transform.edc.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToDataplaneMetadataTransformer extends AbstractJsonLdTransformer<JsonObject, DataplaneMetadata> {

    public JsonObjectToDataplaneMetadataTransformer() {
        super(JsonObject.class, DataplaneMetadata.class);
    }

    @Override
    public @Nullable DataplaneMetadata transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataplaneMetadata.Builder.newInstance();

        var labels = jsonObject.getJsonArray(DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS);
        if (labels != null) {
            transformArray(labels, Object.class, context).forEach(label -> builder.label(label.toString()));
        }

        var properties = jsonObject.getJsonArray(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES);
        if (properties != null) {
            properties.forEach(v -> visitProperties(v.asJsonObject(), key -> value ->
                    builder.property(key, transformGenericProperty(value, context))));
        }

        return builder.build();
    }
}
