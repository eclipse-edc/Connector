/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.signaling.transform.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.signaling.transform.DspaceDataAddressSerialization;
import org.eclipse.edc.connector.api.signaling.transform.DspaceEndpointProperty;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.eclipse.edc.connector.api.signaling.transform.DspaceDataAddressSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY;
import static org.eclipse.edc.connector.api.signaling.transform.DspaceDataAddressSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * Transforms a {@link JsonObject} into a DataAddress using the DSPACE-serialization format.
 */
public class JsonObjectToDataAddressTransformer extends AbstractJsonLdTransformer<JsonObject, DataAddress> {
    public JsonObjectToDataAddressTransformer() {
        super(JsonObject.class, DataAddress.class);
    }

    @Override
    public @Nullable DataAddress transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataAddress.Builder.newInstance();
        visitProperties(jsonObject, (s, v) -> transformProperties(s, v, builder, context));
        return builder.build();
    }

    private void transformProperties(String key, JsonValue jsonValue, DataAddress.Builder builder, TransformerContext context) {
        switch (key) {
            case DspaceDataAddressSerialization.ENDPOINT_PROPERTY ->
                    transformString(jsonValue, endpoint -> builder.property("endpoint", endpoint), context);
            case DspaceDataAddressSerialization.ENDPOINT_TYPE_PROPERTY -> {
                var endpointType = transformString(jsonValue, context);
                builder.type(endpointType);
            }
            case DspaceDataAddressSerialization.ENDPOINT_PROPERTIES_PROPERTY ->
                    transformEndpointProperties(jsonValue, ep -> builder.property(ep.name(), ep.value()));
            default -> throw new IllegalArgumentException("Unexpected value: " + key);
        }
    }

    /**
     * This method transforms a {@code dspace:EndpointProperties} array, which consists of {@code dspace:EndpointProperty} entries
     * and invokes a consumer for each of those entries.
     *
     * @param jsonValue The endpointProperties JsonArray
     * @param consumer  A consumer that takes the {@link DspaceEndpointProperty} and processes it.
     */
    private void transformEndpointProperties(JsonValue jsonValue, Consumer<DspaceEndpointProperty> consumer) {
        Function<JsonObject, DspaceEndpointProperty> converter = (jo) -> {
            var name = jo.getJsonArray(ENDPOINT_PROPERTY_NAME_PROPERTY).get(0).asJsonObject().get(VALUE);
            var value = jo.getJsonArray(ENDPOINT_PROPERTY_VALUE_PROPERTY).get(0).asJsonObject().get(VALUE);
            return new DspaceEndpointProperty(((JsonString) name).getString(), ((JsonString) value).getString());
        };
        if (jsonValue instanceof JsonObject object) {
            consumer.accept(converter.apply(object));
        }
        if (jsonValue instanceof JsonArray array) {
            // invoke the method recursively for every dspace:EndpointProperty entry
            array.forEach(jv -> transformEndpointProperties(jv, consumer));
        }
    }


}
