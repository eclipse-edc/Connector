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

package org.eclipse.edc.protocol.dsp.transform.transformer;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.protocol.dsp.transform.transformer.JsonLdKeywords.ID;
import static org.eclipse.edc.protocol.dsp.transform.transformer.JsonLdKeywords.KEYWORDS;
import static org.eclipse.edc.protocol.dsp.transform.transformer.JsonLdKeywords.VALUE;

/**
 * Base JSON-LD transformer implementation.
 */
public abstract class AbstractJsonLdTransformer<INPUT, OUTPUT> implements JsonLdTransformer<INPUT, OUTPUT> {
    private final Class<INPUT> input;
    private final Class<OUTPUT> output;
    
    protected AbstractJsonLdTransformer(Class<INPUT> input, Class<OUTPUT> output) {
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
    
    protected void visitProperties(JsonObject object, BiConsumer<String, JsonValue> consumer) {
        object.entrySet().stream().filter(entry -> !KEYWORDS.contains(entry.getKey())).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }
    
    protected Object transformGenericProperty(JsonValue value, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            if (jsonArray.size() == 1) {
                // unwrap array
                return context.transform(jsonArray.get(0), Object.class);
            } else {
                return jsonArray.stream().map(prop -> context.transform(prop, Object.class)).collect(toList());
            }
        } else {
            return context.transform(value, Object.class);
        }
    }
    
    /**
     * Transforms a JsonValue to a string and applies the result function. If the value parameter
     * is not of type JsonString, JsonObject or JsonArray, a problem is reported to the context.
     *
     * @param value the value to transform
     * @param resultFunction the function to apply to the transformation result
     * @param context the transformer context
     */
    protected void transformString(JsonValue value, Consumer<String> resultFunction, TransformerContext context) {
        if (value instanceof JsonString) {
            var result = ((JsonString) value).getString();
            resultFunction.accept(result);
        } else if (value instanceof JsonObject) {
            var jsonString = ((JsonObject) value).getJsonString(VALUE);
            transformString(jsonString, resultFunction, context);
        } else if (value instanceof JsonArray) {
            transformString(((JsonArray) value).get(0), resultFunction, context);
        } else {
            context.reportProblem(format("Invalid property. Expected JsonString but got %s",
                    value.getClass().getSimpleName()));
        }
    }
    
    /**
     * Transforms a JsonValue to the desired output type. The result can be a single instance or a
     * list of that type, depending on whether the given value is a JsonObject or a JsonArray. The
     * result function is applied to every instance. If the value parameter is neither of type
     * JsonObject nor JsonArray, a problem is reported to the context.
     *
     * @param value the value to transform
     * @param type the desired result type
     * @param resultFunction the function to apply to the transformation result, if it is a single instance
     * @param context the transformer context
     * @param <T> the desired result type
     */
    protected <T> void transformArrayOrObject(JsonValue value, Class<T> type, Consumer<T> resultFunction,
                                              TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            jsonArray.stream().map(entry -> context.transform(entry, type)).forEach(resultFunction::accept);
        } else if (value instanceof JsonObject) {
            var result = context.transform(value, type);
            resultFunction.accept(result);
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject or JsonArray but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
        }
    }
    
    /**
     * Returns the {@code @id} of the JSON object.
     */
    protected String nodeId(JsonObject object) {
        var id = object.get(ID);
        return id instanceof JsonString ? ((JsonString) id).getString() : null;
    }
    
    /**
     * Returns the {@code @type} of the JSON object. If more than one type is specified, this method will return the first. For multiple types, {@see #nodeTypes}.
     */
    protected String nodeType(JsonObject object, TransformerContext context) {
        var typeNode = object.get(JsonLdKeywords.TYPE);
        if (typeNode == null) {
            context.reportProblem("Property @type not found on JSON Object");
            return null;
        }
        
        if (typeNode instanceof JsonString) {
            return ((JsonString) typeNode).getString();
        } else if (typeNode instanceof JsonArray) {
            var array = typeValueArray(typeNode, context);
            if (array == null) {
                return null;
            }
            var typeValue = array.get(0); // a note can have more than one type, take the first
            if (!(typeValue instanceof JsonString)) {
                context.reportProblem("Expected @type value to be a string");
                return null;
            }
            return ((JsonString) typeValue).getString();
        }
        
        context.reportProblem("Expected @type value to be either string or array");
        return null;
    }
    
    @Nullable
    protected JsonArray typeValueArray(JsonValue typeNode, TransformerContext context) {
        if (!(typeNode instanceof JsonArray)) {
            context.reportProblem("Invalid @type node: " + typeNode.getValueType());
            return null;
        }
        var array = (JsonArray) typeNode;
        if (array.isEmpty()) {
            context.reportProblem("Expected @type node to be an array with at least one element");
            return null;
        }
        return array;
    }
}
