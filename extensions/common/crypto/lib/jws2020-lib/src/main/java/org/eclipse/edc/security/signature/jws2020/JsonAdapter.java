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

import com.apicatalog.ld.node.adapter.LdAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import java.util.Map;

class JsonAdapter implements LdAdapter<Object> {
    private final ObjectMapper mapper;

    JsonAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public Object read(JsonObject value) {
        return mapper.convertValue(value, Object.class);
    }

    @Override
    public JsonObject write(Object value) {
        if (value instanceof Map) {
            var jo = Json.createObjectBuilder();
            jo.add(JsonLdKeywords.VALUE, Json.createObjectBuilder((Map) value));
            jo.add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON);
            return mapper.convertValue(jo.build(), JsonObject.class);
        }
        return mapper.convertValue(value, JsonObject.class);
    }
}
