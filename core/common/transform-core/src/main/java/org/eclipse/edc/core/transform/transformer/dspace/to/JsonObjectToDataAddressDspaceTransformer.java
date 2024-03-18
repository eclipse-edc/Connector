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

package org.eclipse.edc.core.transform.transformer.dspace.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.eclipse.edc.core.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY;
import static org.eclipse.edc.core.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY;
import static org.eclipse.edc.core.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY;
import static org.eclipse.edc.core.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY;
import static org.eclipse.edc.core.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY;

/**
 * Transforms a {@link JsonObject} into a DataAddress using the DSPACE-serialization format.
 */
public class JsonObjectToDataAddressDspaceTransformer extends AbstractJsonLdTransformer<JsonObject, DataAddress> {
    public JsonObjectToDataAddressDspaceTransformer() {
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
            case ENDPOINT_PROPERTY -> { }
            case ENDPOINT_TYPE_PROPERTY -> builder.type(transformString(jsonValue, context));
            case ENDPOINT_PROPERTIES_PROPERTY ->
                    transformEndpointProperties(jsonValue, ep -> builder.property(ep.name(), ep.value()), context);
            default -> throw new IllegalArgumentException("Unexpected value: " + key);
        }
    }

    /**
     * This method transforms a {@code dspace:EndpointProperties} array, which consists of {@code dspace:EndpointProperty} entries
     * and invokes a consumer for each of those entries.
     *
     * @param jsonValue The endpointProperties JsonArray
     * @param consumer  A consumer that takes the {@link DspaceEndpointProperty} and processes it.
     * @param context   the transformer context, to which this method delegates when transforming strings.
     */
    private void transformEndpointProperties(JsonValue jsonValue, Consumer<DspaceEndpointProperty> consumer, TransformerContext context) {
        Function<JsonObject, DspaceEndpointProperty> converter = (jo) -> {
            var name = transformString(jo.get(ENDPOINT_PROPERTY_NAME_PROPERTY), context);
            var value = transformString(jo.get(ENDPOINT_PROPERTY_VALUE_PROPERTY), context);
            return new DspaceEndpointProperty(name, value);
        };
        if (jsonValue instanceof JsonObject object) {
            consumer.accept(converter.apply(object));
        }
        if (jsonValue instanceof JsonArray array) {
            // invoke the method recursively for every dspace:EndpointProperty entry
            array.forEach(jv -> transformEndpointProperties(jv, consumer, context));
        }
    }

    //container to hold endpoint property objects 
    private record DspaceEndpointProperty(String name, String value) {
    }
}
