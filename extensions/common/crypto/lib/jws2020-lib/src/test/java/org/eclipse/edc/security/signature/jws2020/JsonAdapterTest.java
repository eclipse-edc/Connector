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

package org.eclipse.edc.security.signature.jws2020;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAdapterTest {

    private final JsonAdapter adapter = new JsonAdapter(JacksonJsonLd.createObjectMapper());

    @Test
    void read() {
        var jo = Json.createValue("foobar");
        var result = adapter.read(jo);
        assertThat(result).isEqualTo("foobar");
    }

    @Test
    void read_jsonObjectWithValue() {
        var jo = Json.createObjectBuilder()
                .add("@type", "test-type")
                .add("@value", "test-value")
                .build();
        var result = adapter.read(jo);
        assertThat(result).isEqualTo("test-value");
    }

    @Test
    void write() {
        var obj = "test-string";
        var result = adapter.write(obj);
        assertThat(result).isInstanceOf(JsonString.class);
    }

    @Test
    void write_map() {
        var map = Map.of("key1", "value1", "key2", "value2");
        var result = adapter.write(map);
        assertThat(result).isInstanceOf(JsonObject.class).extracting(JsonValue::asJsonObject).matches(jo -> jo.size() == 2);
    }
}