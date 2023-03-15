/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class TransformerUtil {
    
    private TransformerUtil() { }
    
    public static Object transformGenericProperty(JsonValue value, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            if (jsonArray.isEmpty()) {
                return List.of();
            } else if (jsonArray.size() == 1) {
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
     * is not of type JsonString, a problem is reported to the context.
     *
     * @param value the value to transform
     * @param resultFunction the function to apply to the transformation result
     * @param context the transformer context
     */
    public static void transformString(JsonValue value, Consumer<String> resultFunction, TransformerContext context) {
        if (value instanceof JsonString) {
            var result = ((JsonString) value).getString();
            resultFunction.accept(result);
        } else {
            context.reportProblem(format("Invalid property. Expected JsonString but got %s",
                    value.getClass().getSimpleName()));
        }
    }
    
    /**
     * Transforms a JsonValue to the desired output type and applies the result function. If the
     * value parameter is not of type JsonObject, a problem is reported to the context.
     *
     * @param value the value to transform
     * @param type the desired result type
     * @param resultFunction the function to apply to the transformation result
     * @param context the transformer context
     * @param <T> the desired result type
     */
    public static <T> void transformObject(JsonValue value, Class<T> type, Consumer<T> resultFunction, TransformerContext context) {
        if (value instanceof JsonObject) {
            var result = context.transform((JsonObject) value, type);
            resultFunction.accept(result);
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
        }
    }
    
    /**
     * Transforms a JsonValue to the desired output type. The result can be a single instance or a
     * list of that type, depending on whether the given value is a JsonObject or a JsonArray. If
     * the result is a single instance, the result function is applied. If it is a list, the list
     * result function is applied. If the value parameter is neither of type JsonObject nor
     * JsonArray, a problem is reported to the context.
     *
     * @param value the value to transform
     * @param type the desired result type
     * @param resultFunction the function to apply to the transformation result, if it is a single instance
     * @param listResultFunction the function to apply to the transformation result, if it is a list
     * @param context the transformer context
     * @param <T> the desired result type
     */
    public static <T> void transformArrayOrObject(JsonValue value, Class<T> type, Consumer<T> resultFunction,
                                                  Consumer<List<T>> listResultFunction, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var results = jsonArray.stream().map(entry -> context.transform(entry, type)).collect(toList());
            listResultFunction.accept(results);
        } else if (value instanceof JsonObject) {
            var result = context.transform(value, type);
            resultFunction.accept(result);
        } else {
            context.reportProblem(format("Invalid property of type %s. Expected JsonObject or JsonArray but got %s",
                    type.getSimpleName(), value.getClass().getSimpleName()));
        }
    }
}
