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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectToDataplaneMetadataTransformer extends AbstractJsonLdTransformer<JsonObject, DataplaneMetadata> {

    public JsonObjectToDataplaneMetadataTransformer() {
        super(JsonObject.class, DataplaneMetadata.class);
    }

    @Override
    public @Nullable DataplaneMetadata transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataplaneMetadata.Builder.newInstance();

        var labels = jsonObject.getJsonArray(DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS);
        if (labels != null) {
            labels.stream()
                    .map(this::nodeJsonValue)
                    .map(value -> deserializeLabel(value, context))
                    .filter(Objects::nonNull)
                    .forEach(builder::label);
        }

        var properties = jsonObject.get(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES);
        if (properties instanceof JsonArray propertiesArray && !propertiesArray.isEmpty()) {

            var propertiesEntry = propertiesArray.get(0);
            if (propertiesEntry instanceof JsonObject object) {
                var map = Optional.ofNullable(object.getJsonString(TYPE))
                        .map(JsonString::getString)
                        .filter(it -> it.equals(JSON))
                        .map(i -> nodeJsonValue(propertiesEntry).asJsonObject())
                        .orElse(object);

                visitProperties(map, (key, value) -> builder.property(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }

        return builder.build();
    }

    private @Nullable String deserializeLabel(JsonValue value, @NotNull TransformerContext context) {
        if (value instanceof JsonString string) {
            return string.getString();
        }

        context.reportProblem("DataplaneMetadata labels should be strings, but label '%s' is a '%s'".formatted(value, value.getValueType()));
        return null;
    }
}
