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

package org.eclipse.edc.validator.jsonobject;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.stream.Stream;

public enum JsonWalkers implements JsonWalker {

    ROOT_OBJECT {
        @Override
        public Stream<JsonObject> extract(JsonObject object, JsonLdPath path) {
            return Stream.of(object);
        }
    },

    NESTED_OBJECT {
        @Override
        public Stream<JsonObject> extract(JsonObject object, JsonLdPath path) {
            var array = object.getJsonArray(path.last());

            if (array == null) {
                return Stream.empty();
            } else {
                return Stream.of(array.getJsonObject(0));
            }
        }
    },

    ARRAY_ITEMS {
        @Override
        public Stream<JsonObject> extract(JsonObject object, JsonLdPath path) {
            var array = object.getJsonArray(path.last());

            if (array == null) {
                return Stream.empty();
            } else {
                return array.stream().map(JsonValue::asJsonObject);
            }
        }
    }
}
