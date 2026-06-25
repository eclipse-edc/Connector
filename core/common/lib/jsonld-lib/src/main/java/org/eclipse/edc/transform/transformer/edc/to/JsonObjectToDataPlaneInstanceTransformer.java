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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_DEST_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_TRANSFER_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.Builder;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DESTINATION_PROVISION_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.LAST_ACTIVE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.PROPERTIES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.TURN_COUNT;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

public class JsonObjectToDataPlaneInstanceTransformer extends AbstractJsonLdTransformer<JsonObject, DataPlaneInstance> {
    public JsonObjectToDataPlaneInstanceTransformer() {
        super(JsonObject.class, DataPlaneInstance.class);
    }

    @Override
    public @Nullable DataPlaneInstance transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Builder.newInstance()
                .id(nodeId(jsonObject))
                .url(transformUrl(jsonObject.get(URL), context))
                .lastActive(transformLong(jsonObject.get(LAST_ACTIVE), context))
                .allowedSourceTypes(asSetOfStrings(jsonObject.get(ALLOWED_SOURCE_TYPES), context))
                .allowedTransferType(asSetOfStrings(jsonObject.get(ALLOWED_TRANSFER_TYPES), context))
                .destinationProvisionTypes(asSetOfStrings(jsonObject.get(DESTINATION_PROVISION_TYPES), context))
                .stateTimestamp(transformLong(jsonObject.get(DATAPLANE_INSTANCE_STATE_TIMESTAMP), context))
                .turnCount(transformInt(jsonObject.get(TURN_COUNT), context))
                .allowedDestTypes(asSetOfStrings(jsonObject.get(ALLOWED_DEST_TYPES), context));

        var properties = jsonObject.get(PROPERTIES);
        if (properties != null) {
            visitProperties(properties.asJsonArray().getJsonObject(0), (k, val) -> builder.property(k, transformGenericProperty(val, context)));
        }

        return builder.build();
    }

    private URL transformUrl(JsonValue value, @NotNull TransformerContext context) {
        try {
            return new URL(Objects.requireNonNull(transformString(value, context)));
        } catch (MalformedURLException e) {
            context.reportProblem(e.getMessage());
            return null;
        }
    }

    private @NotNull Set<String> asSetOfStrings(JsonValue jsonValue, TransformerContext context) {
        if (jsonValue == null) {
            return emptySet();
        }
        return jsonValue.asJsonArray().stream().map(it -> transformString(it, context)).collect(Collectors.toSet());
    }

    private long transformLong(JsonValue jsonValue, @NotNull TransformerContext context) {
        if (jsonValue == null) {
            return 0;
        }

        if (jsonValue instanceof JsonNumber number) {
            return number.longValue();
        }

        if (jsonValue instanceof JsonArray array) {
            return transformLong(array.getJsonObject(0).get(VALUE), context);
        }

        context.reportProblem("Cannot convert a " + jsonValue.getValueType() + " to a long!");
        return 0;
    }
}
