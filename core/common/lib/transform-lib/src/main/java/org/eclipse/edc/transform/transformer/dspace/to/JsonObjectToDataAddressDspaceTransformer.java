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

package org.eclipse.edc.transform.transformer.dspace.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;

/**
 * Transforms a {@link JsonObject} into a DataAddress using the DSPACE-serialization format.
 */
public class JsonObjectToDataAddressDspaceTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, DataAddress> {

    public JsonObjectToDataAddressDspaceTransformer() {
        this(new JsonLdNamespace(DSPACE_SCHEMA));
    }

    public JsonObjectToDataAddressDspaceTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, DataAddress.class, namespace);
    }

    @Override
    public @Nullable DataAddress transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataAddress.Builder.newInstance();
        transformString(jsonObject.get(forNamespace(ENDPOINT_TYPE_PROPERTY_TERM)), builder::type, context);
        transformEndpointProperties(jsonObject.get(forNamespace(ENDPOINT_PROPERTIES_PROPERTY_TERM)), ep -> builder.property(ep.name(), ep.value()), context);
        return builder.build();
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
            var name = transformString(jo.get(forNamespace(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM)), context);
            var value = transformString(jo.get(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM)), context);
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
