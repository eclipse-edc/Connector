/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JacksonSerializer implements Serializer<JsonNode> {

    private ObjectMapper mapper;

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        mapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(String topic, JsonNode data) {
        if (data == null) {
            return null;
        }

        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
    }
}