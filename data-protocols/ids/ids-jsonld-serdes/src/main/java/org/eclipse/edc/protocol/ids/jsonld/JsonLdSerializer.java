/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.ids.jsonld;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Custom Jackson serializer for any {@link Object}. Adds type and context information to result object.
 *
 * @param <T> The object that should be serialized.
 */
public class JsonLdSerializer<T> extends JsonSerializer<T> {
    private final Class<T> type;
    private final String contextInformation;

    private static final ThreadLocal<Integer> CURRENT_RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    public JsonLdSerializer(Class<T> type, String contextInformation) {
        this.type = type;
        this.contextInformation = contextInformation;
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        CURRENT_RECURSION_DEPTH.set(CURRENT_RECURSION_DEPTH.get() + 1);

        Map<String, Object> propertiesMap = null;
        try {
            var field = value.getClass().getDeclaredField("properties");
            field.setAccessible(true);
            propertiesMap = (Map<String, Object>) field.get(value);
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
            // empty
        }
        removeProperty(value, "properties");

        // remove properties "comment" and "label"
        removeProperty(value, "comment");
        removeProperty(value, "label");

        generator.writeStartObject();

        // write new object
        var serializer = instantiateSerializerFromProvider(provider, type);
        serializer.unwrappingSerializer(null).serialize(value, generator, provider);

        if (CURRENT_RECURSION_DEPTH.get() == 1) {
            // context needed only once (for parent object)
            generator.writeObjectField("@context", contextInformation);
        }

        // add type property
        var type = getTypeName(value.getClass());
        if (type != null) {
            generator.writeObjectField("@type", type);
        }

        // add custom properties as root properties (not in a separate "properties" map)
        if (propertiesMap != null) {
            for (var key : propertiesMap.keySet()) {
                var val = propertiesMap.get(key);
                if (val instanceof URI) {
                    generator.writeStringField(key, val.toString());
                } else {
                    generator.writeObjectField(key, val);
                }
            }
        }

        generator.writeEndObject();

        CURRENT_RECURSION_DEPTH.set(CURRENT_RECURSION_DEPTH.get() - 1);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider provider, TypeSerializer ser) throws IOException {
        serialize(value, gen, provider);
    }

    private void removeProperty(Object value, String name) {
        try {
            var field = value.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(value, null);
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
            // empty
        }
    }

    private String getTypeName(Class<?> clazz) {
        var typeName = clazz.getAnnotation(JsonTypeName.class);
        if (typeName != null) {
            var value = typeName.value();
            if (value == null) {
                getTypeName(clazz.getSuperclass());
            }
            return value;
        }
        return null;
    }

    private JsonSerializer<Object> instantiateSerializerFromProvider(SerializerProvider provider, Class<T> type) throws JsonMappingException {
        var javaType = provider.constructType(type);
        var beanDescription = provider.getConfig().introspect(javaType);
        var staticTyping = provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
        return BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDescription, staticTyping);
    }
}
