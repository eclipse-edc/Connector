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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

import java.util.HashMap;
import java.util.Map;


public class JacksonTypeManager implements TypeManager {

    /**
     * Concurrent support is not needed since this map is only populated a boot, which is single-threaded.
     *
     * <p>Since Jackson 3 mappers are immutable, runtime registration of types and serializers is implemented by
     * rebuilding the affected mapper(s) via {@link ObjectMapper#rebuild()} and replacing the stored instance.
     */
    private final Map<String, ObjectMapper> objectMappers = new HashMap<>();

    /**
     * Default constructor.
     */
    public JacksonTypeManager() {
        var defaultMapper = JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, true)
                .build();

        registerContext(DEFAULT_TYPE_CONTEXT, defaultMapper);
    }

    @Override
    public ObjectMapper getMapper() {
        return getMapper(DEFAULT_TYPE_CONTEXT);
    }

    @Override
    @NotNull
    public ObjectMapper getMapper(String key) {
        return objectMappers.computeIfAbsent(key, k -> objectMappers.get(DEFAULT_TYPE_CONTEXT).rebuild().build());
    }

    @Override
    public void registerContext(String key, ObjectMapper mapper) {
        objectMappers.put(key, mapper);
    }

    @Override
    public void registerTypes(Class<?>... type) {
        objectMappers.replaceAll((k, m) -> m.rebuild().registerSubtypes(type).build());
    }

    @Override
    public void registerTypes(NamedType... type) {
        objectMappers.replaceAll((k, m) -> m.rebuild().registerSubtypes(type).build());
    }

    @Override
    public void registerTypes(String key, Class<?>... type) {
        objectMappers.put(key, getMapper(key).rebuild().registerSubtypes(type).build());
    }

    @Override
    public void registerTypes(String key, NamedType... type) {
        objectMappers.put(key, getMapper(key).rebuild().registerSubtypes(type).build());
    }

    @Override
    public <T> void registerSerializer(String key, Class<T> type, ValueSerializer<T> serializer) {
        var module = new SimpleModule();
        module.addSerializer(type, serializer);
        objectMappers.put(key, getMapper(key).rebuild().addModule(module).build());
    }

    @Override
    public <T> void registerSerializer(Class<T> type, ValueSerializer<T> serializer) {
        registerSerializer(DEFAULT_TYPE_CONTEXT, type, serializer);
    }

    @Override
    public <T> T readValue(String input, TypeReference<T> typeReference) {
        try {
            return getMapper().readValue(input, typeReference);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> T readValue(String input, Class<T> type) {
        try {
            return getMapper().readValue(input, type);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> type) {
        try {
            return getMapper().readValue(bytes, type);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public String writeValueAsString(Object value) {
        try {
            return getMapper().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        try {
            return getMapper().writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public String writeValueAsString(Object value, TypeReference<?> reference) {
        try {
            return getMapper().writerFor(reference).writeValueAsString(value);
        } catch (JacksonException e) {
            throw new EdcException(e);
        }
    }
}
