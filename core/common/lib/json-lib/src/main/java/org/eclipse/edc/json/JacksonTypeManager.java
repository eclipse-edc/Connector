/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class JacksonTypeManager implements TypeManager {
    final ObjectMapper defaultMapper;

    /**
     * Concurrent support is not needed since this map is only populated a boot, which is single-threaded.
     */
    final Map<String, ObjectMapper> objectMappers = new HashMap<>();

    /**
     * Default constructor.
     */
    public JacksonTypeManager() {
        defaultMapper = new ObjectMapper();
        defaultMapper.registerModule(new JavaTimeModule()); // configure ISO 8601 time de/serialization
        defaultMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // serialize dates in ISO 8601 format
        defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        registerContext(DEFAULT_TYPE_CONTEXT, defaultMapper);
    }

    @Override
    public ObjectMapper getMapper() {
        return defaultMapper;
    }

    @Override
    @NotNull
    public ObjectMapper getMapper(String key) {
        return objectMappers.computeIfAbsent(key, k -> defaultMapper.copy());
    }

    @Override
    public void registerContext(String key, ObjectMapper mapper) {
        objectMappers.put(key, mapper);
    }

    @Override
    public void registerTypes(Class<?>... type) {
        objectMappers.values().forEach(m -> m.registerSubtypes(type));
    }

    @Override
    public void registerTypes(NamedType... type) {
        objectMappers.values().forEach(m -> m.registerSubtypes(type));
    }

    @Override
    public void registerTypes(String key, Class<?>... type) {
        getMapper(key).registerSubtypes(type);
    }

    @Override
    public void registerTypes(String key, NamedType... type) {
        getMapper(key).registerSubtypes(type);
    }

    @Override
    public <T> void registerSerializer(String key, Class<T> type, JsonSerializer<T> serializer) {
        var module = new SimpleModule();
        module.addSerializer(type, serializer);
        getMapper(key).registerModule(module);
    }

    @Override
    public <T> void registerSerializer(Class<T> type, JsonSerializer<T> serializer) {
        var module = new SimpleModule();
        module.addSerializer(type, serializer);
        getMapper().registerModule(module);
    }

    @Override
    public <T> T readValue(String input, TypeReference<T> typeReference) {
        try {
            return getMapper().readValue(input, typeReference);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> T readValue(String input, Class<T> type) {
        try {
            return getMapper().readValue(input, type);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> type) {
        try {
            return getMapper().readValue(bytes, type);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public String writeValueAsString(Object value) {
        try {
            return getMapper().writeValueAsString(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        try {
            return getMapper().writeValueAsBytes(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public String writeValueAsString(Object value, TypeReference<?> reference) {
        try {
            return getMapper().writerFor(reference).writeValueAsString(value);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
