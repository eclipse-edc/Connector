/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.jsonld.spi.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;

import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * Provides utility methods for json-from-model type transformers
 *
 * @param <INPUT> input type
 * @param <OUTPUT> output type
 */
public abstract class JsonLdFromModelTransformer<INPUT, OUTPUT> implements JsonLdTransformer<INPUT, OUTPUT> {

    private final Class<INPUT> input;
    private final Class<OUTPUT> output;

    protected JsonLdFromModelTransformer(Class<INPUT> input, Class<OUTPUT> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Class<INPUT> getInputType() {
        return input;
    }

    @Override
    public Class<OUTPUT> getOutputType() {
        return output;
    }

    /**
     * Add JSON-LD ID object to the builder only if the id is not null, to avoid NPE.
     *
     * @param id      the value.
     * @param key     the key.
     * @param builder the builder.
     */
    protected void addIdIfNotNull(String id, String key, JsonBuilderFactory factory, JsonObjectBuilder builder) {
        if (id != null) {
            builder.add(key, createId(factory, id));
        }
    }

    /**
     * Create a JSON-LD ID object with the input string.
     *
     * @param factory The {@link JsonBuilderFactory} .
     * @param id      The id.
     */
    protected JsonObject createId(JsonBuilderFactory factory, String id) {
        return factory.createObjectBuilder().add(ID, id).build();
    }

    /**
     * Add a key-value pair to the builder only if the value is not null, to avoid NPE.
     *
     * @param value   the value.
     * @param key     the key.
     * @param builder the builder.
     */
    protected void addIfNotNull(String value, String key, JsonObjectBuilder builder) {
        if (value != null) {
            builder.add(key, value);
        }
    }

    /**
     * Transforms properties of a Java type. The properties are mapped to generic JSON values.
     *
     * @param properties the properties to map
     * @param builder    the builder on which to set the properties
     * @param mapper     the mapper for converting the properties
     * @param context    the transformer context
     */
    protected void transformProperties(Map<String, ?> properties, JsonObjectBuilder builder, ObjectMapper mapper, TransformerContext context) {
        Function<Object, JsonValue> func = v -> mapper.convertValue(v, JsonValue.class);

        transformProperties(properties, builder, func, context);
    }

    /**
     * Transforms properties of a Java type. The properties are mapped to generic JSON values using the provided transformer function.
     *
     * @param properties          the properties to map
     * @param builder             the builder on which to set the properties
     * @param transformerFunction the function to transform the property value
     * @param context             the transformer context
     */
    protected void transformProperties(Map<String, ?> properties, JsonObjectBuilder builder, Function<Object, JsonValue> transformerFunction, TransformerContext context) {
        if (properties == null) {
            return;
        }
        properties.forEach((k, v) -> {
            try {
                builder.add(k, transformerFunction.apply(v));
            } catch (IllegalArgumentException e) {
                context.problem()
                        .invalidProperty()
                        .type(JsonLdKeywords.VALUE)
                        .property(k)
                        .value(v != null ? v.toString() : "null")
                        .error(e.getMessage())
                        .report();
            }
        });
    }
}
