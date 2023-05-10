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

package org.eclipse.edc.jsonld.spi.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.KEYWORDS;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

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

    /**
     * Transforms properties of a Java type. The properties are mapped to generic JSON values.
     *
     * @param properties the properties to map
     * @param builder    the builder on which to set the properties
     * @param mapper     the mapper for converting the properties
     * @param context    the transformer context
     */
    protected void transformProperties(Map<String, ?> properties, JsonObjectBuilder builder, ObjectMapper mapper, TransformerContext context) {
        if (properties == null) {
            return;
        }

        properties.forEach((k, v) -> {
            try {
                builder.add(k, mapper.convertValue(v, JsonValue.class));
            } catch (IllegalArgumentException e) {
                context.reportProblem(format("Failed to transform property: %s", e.getMessage()));
            }
        });
    }

    protected void visitProperties(JsonObject object, BiConsumer<String, JsonValue> consumer) {
        object.entrySet().stream().filter(entry -> !KEYWORDS.contains(entry.getKey())).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }

    protected void visitProperties(JsonObject object, Function<String, Consumer<JsonValue>> consumer) {
        object.entrySet().stream()
                .filter(entry -> !KEYWORDS.contains(entry.getKey()))
                .forEach(entry -> consumer.apply(entry.getKey()).accept(entry.getValue()));
    }

    @NotNull
    protected static Consumer<JsonValue> doNothing() {
        return v -> {
        };
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
     * @param value          the value to transform
     * @param resultFunction the function to apply to the transformation result
     * @param context        the transformer context
     */
    protected void transformString(JsonValue value, Consumer<String> resultFunction, TransformerContext context) {
        resultFunction.accept(transformString(value, context));
    }

    /**
     * Transforms a mandatory JsonValue to a string and applies the result function. If the value parameter
     * is not of type JsonString, JsonObject or JsonArray, a problem is reported to the context.
     *
     * @param value          the value to transform
     * @param resultFunction the function to apply to the transformation result
     * @param context        the transformer context
     * @return true if the string was present
     */
    protected boolean transformMandatoryString(JsonValue value, Consumer<String> resultFunction, TransformerContext context) {
        var result = transformString(value, context);
        if (result == null) {
            return false;
        }
        resultFunction.accept(result);
        return true;
    }

    /**
     * Transforms a JsonValue to a string and applies the result function. If the value parameter
     * is not of type JsonString, JsonObject or JsonArray, a problem is reported to the context.
     *
     * @param value   the value to transform
     * @param context the transformer context
     * @return the string result
     */
    @Nullable
    protected String transformString(JsonValue value, TransformerContext context) {
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        } else if (value instanceof JsonObject) {
            var object = value.asJsonObject();
            return Stream.of(VALUE, ID).map(object::get)
                    .filter(Objects::nonNull)
                    .findFirst().map(it -> transformString(it, context))
                    .orElseGet(() -> {
                        context.reportProblem(format("Invalid property. Expected to find one of @value, @id in JsonObject but got %s", value));
                        return null;
                    });
        } else if (value instanceof JsonArray) {
            return transformString(((JsonArray) value).get(0), context);
        } else {
            context.reportProblem(format("Invalid property. Expected JsonString, JsonObject or JsonArray but got %s",
                    Optional.ofNullable(value).map(it -> getClass()).map(Class::getSimpleName).orElse(null)));
            return null;
        }
    }

    /**
     * Transforms a JsonValue to int. If the value parameter is not of type JsonNumber, JsonObject or JsonArray,
     * a problem is reported to the context.
     *
     * @param value   the value to transform
     * @param context the transformer context
     * @return the int value
     */
    protected int transformInt(JsonValue value, TransformerContext context) {
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        } else if (value instanceof JsonObject) {
            var jsonNumber = value.asJsonObject().getJsonNumber(VALUE);
            return transformInt(jsonNumber, context);
        } else if (value instanceof JsonArray) {
            return transformInt(value.asJsonArray().get(0), context);
        } else {
            context.reportProblem(format("Invalid property. Expected JsonNumber, JsonObject or JsonArray but got %s",
                    value.getClass().getSimpleName()));
            return 0;
        }
    }

    /**
     * Transforms a JsonValue to boolean. If the value parameter is not of type JsonObject or JsonArray,
     * a problem is reported to the context.
     *
     * @param value the value to transform
     * @param context the transformer context
     * @return the int value
     */
    protected boolean transformBoolean(JsonValue value, TransformerContext context) {
        if (value instanceof JsonObject) {
            return value.asJsonObject().getBoolean(VALUE);
        } else if (value instanceof JsonArray) {
            return transformBoolean(value.asJsonArray().get(0), context);
        } else {
            context.reportProblem(format("Invalid property. Expected JsonObject or JsonArray but got %s",
                    value.getClass().getSimpleName()));
            return false;
        }
    }

    /**
     * Transforms a JsonValue to the desired output type. The result can be a single instance or a
     * list of that type, depending on whether the given value is a JsonObject or a JsonArray. The
     * result function is applied to every instance. If the value parameter is neither of type
     * JsonObject nor JsonArray, a problem is reported to the context.
     *
     * @param value          the value to transform
     * @param type           the desired result type
     * @param resultFunction the function to apply to the transformation result, if it is a single instance
     * @param context        the transformer context
     * @param <T>            the desired result type
     */
    protected <T> void transformArrayOrObject(JsonValue value, Class<T> type, Consumer<T> resultFunction,
                                              TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            jsonArray.stream().map(entry -> context.transform(entry, type)).forEach(resultFunction);
        } else if (value instanceof JsonObject) {
            var result = context.transform(value, type);
            resultFunction.accept(result);
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject or JsonArray but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
        }
    }

    /**
     * Transforms a JsonValue to a List. If the value parameter is not of type JsonArray, a problem is reported to the context.
     *
     * @param value   the value to transform
     * @param type    the desired result type
     * @param context the transformer context
     * @param <T>     the desired result type
     * @return the transformed list, null if the value type was not valid.
     */
    protected <T> List<T> transformArray(JsonValue value, Class<T> type, TransformerContext context) {
        if (value instanceof JsonObject) {
            var transformed = context.transform(value.asJsonObject(), type);
            if (transformed == null) {
                context.reportProblem(format("Invalid property of type %s. Expected JsonObject or JsonArray but got %s",
                        type.getSimpleName(), value.getClass().getSimpleName()));
                return emptyList();
            }
            return List.of(transformed);
        } else if (value instanceof JsonArray) {
            return value.asJsonArray().stream()
                    .map(entry -> context.transform(entry, type))
                    .collect(toList());
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject or JsonArray but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
            return null;
        }
    }

    /**
     * Transforms a JsonValue to the desired output type. If the value parameter is neither of type
     * JsonObject nor JsonArray, a problem is reported to the context.
     *
     * @param value   the value to transform
     * @param type    the desired result type
     * @param context the transformer context
     * @param <T>     the desired result type
     * @return the transformed list
     */
    protected <T> T transformObject(JsonValue value, Class<T> type, TransformerContext context) {
        if (value instanceof JsonArray) {
            return value.asJsonArray().stream()
                    .map(entry -> context.transform(entry, type))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> {
                        context.reportProblem(format("Invalid property of type %s. Cannot map array values %s",
                                type.getSimpleName(), value.getClass().getSimpleName()));
                        return null;
                    });
        } else if (value instanceof JsonObject) {
            return context.transform(value, type);
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
            return null;
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
     * Returns the {@code @type} of the JSON object. If more than one type is specified, this method will return the first.
     */
    protected String nodeType(JsonObject object, TransformerContext context) {
        var typeNode = object.get(TYPE);
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

    /**
     * Tries to return the instance given by a supplier (a builder's build method). If this fails
     * due to validation errors, e.g. a required property is missing, reports a problem to the
     * context.
     *
     * @param builder the supplier
     * @param context the context
     * @return the instance or null
     */
    protected <T> T builderResult(Supplier<T> builder, TransformerContext context) {
        try {
            return builder.get();
        } catch (Exception e) {
            context.reportProblem(format("Failed to construct instance: %s", e.getMessage()));
            return null;
        }
    }
}
