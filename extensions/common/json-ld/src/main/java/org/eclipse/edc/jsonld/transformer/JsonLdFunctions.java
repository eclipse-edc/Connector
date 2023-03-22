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

package org.eclipse.edc.jsonld.transformer;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.ID;

/**
 * Contains methods for processing JSON-LD objects.
 */
public class JsonLdFunctions {

    /**
     * Returns the {@code @id} of the JSON object.
     */
    public static String nodeId(JsonObject object) {
        var id = object.get(ID);
        return id instanceof JsonString ? ((JsonString) id).getString() : null;
    }

    /**
     * Returns the {@code @type} of the JSON object. If more than one type is specified, this method will return the first. For multiple types, {@see #nodeTypes}.
     */
    public static String nodeType(JsonObject object, TransformerContext context) {
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

    /**
     * Returns the {@code @type}s of the JSON object.
     */
    public static List<String> nodeTypes(JsonObject object, TransformerContext context) {
        var typeNode = object.get(JsonLdKeywords.TYPE);
        if (typeNode == null) {
            context.reportProblem("Property @type not found on JSON Object");
            return null;
        }

        if (typeNode instanceof JsonString) {
            return List.of(((JsonString) typeNode).getString());
        } else if (typeNode instanceof JsonArray) {
            var array = typeValueArray(object, context);
            return array == null ? null : array.stream()
                    .filter(JsonString.class::isInstance)
                    .map(JsonValue::toString)
                    .collect(Collectors.toList());
        }

        context.reportProblem("Expected @type value to be either string or array");
        return null;
    }

    @Nullable
    private static JsonArray typeValueArray(JsonValue typeNode, TransformerContext context) {
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

    private JsonLdFunctions() {
    }
}
