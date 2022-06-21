/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;

/**
 * Manages system types and is used to deserialize polymorphic types.
 */
public class TypeManager {
    private final ObjectMapper objectMapper;

    public TypeManager() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // configure ISO 8601 time de/serialization
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // serialize dates in ISO 8601 format
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        objectMapper.registerModule(module);
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

    public void registerTypes(Class<?>... type) {
        objectMapper.registerSubtypes(type);
    }

    public void registerTypes(NamedType... type) {
        objectMapper.registerSubtypes(type);
    }

    public <T> T readValue(String info, TypeReference<T> typeReference) {
        try {
            return getMapper().readValue(info, typeReference);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public <T> T readValue(String info, Class<T> type) {
        try {
            return getMapper().readValue(info, type);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public <T> T readValue(byte[] bytes, Class<T> type) {
        try {
            return getMapper().readValue(bytes, type);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public String writeValueAsString(Object value) {
        try {
            return getMapper().writeValueAsString(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public byte[] writeValueAsBytes(Object value) {
        try {
            return getMapper().writeValueAsBytes(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public String writeValueAsString(Object value, TypeReference<?> reference) {
        try {
            return getMapper().writerFor(reference).writeValueAsString(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}


