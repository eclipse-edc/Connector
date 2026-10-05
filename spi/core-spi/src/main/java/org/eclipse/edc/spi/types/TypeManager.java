/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.spi.types;

import org.jetbrains.annotations.NotNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.NamedType;

/**
 * Manages system types and is used to deserialize polymorphic types.
 */
public interface TypeManager {

    String DEFAULT_TYPE_CONTEXT = "default";

    /**
     * Returns the object mapper for the default serialization context.
     */
    ObjectMapper getMapper();

    /**
     * Returns the object mapper for the given serialization context, creating one based on the default mapper if required.
     */
    @NotNull
    ObjectMapper getMapper(String key);

    /**
     * Add custom mapper by key to list of object mappers.
     */
    void registerContext(String key, ObjectMapper mapper);

    /**
     * Registers types with all contexts.
     */
    void registerTypes(Class<?>... type);

    /**
     * Registers types with all contexts.
     */
    void registerTypes(NamedType... type);

    /**
     * Registers types with a context.
     */
    void registerTypes(String key, Class<?>... type);

    /**
     * Registers types with a context.
     */
    void registerTypes(String key, NamedType... type);

    /**
     * Registers a serializer for the given type with a context.
     */
    <T> void registerSerializer(String key, Class<T> type, ValueSerializer<T> serializer);

    /**
     * Registers a serializer for the given type with the default context.
     */
    <T> void registerSerializer(Class<T> type, ValueSerializer<T> serializer);

    /**
     * Read value from string by type.
     */
    <T> T readValue(String input, TypeReference<T> typeReference);

    <T> T readValue(String input, Class<T> type);

    <T> T readValue(byte[] bytes, Class<T> type);

    String writeValueAsString(Object value);

    byte[] writeValueAsBytes(Object value);

    String writeValueAsString(Object value, TypeReference<?> reference);
}
