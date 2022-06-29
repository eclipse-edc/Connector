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

package org.eclipse.dataspaceconnector.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.dataspaceconnector.serializer.calendar.XmlGregorianCalendarModule;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * Service for de-/serializing java objects to/from JSON-LD string using a customized {@link ObjectMapper}.
 */
public class JsonldSerDes {
    private final Monitor monitor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String context;
    private Class<?>[] subtypes;

    /**
     * Constructor.
     *
     * @param monitor monitoring service.
     */
    public JsonldSerDes(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Convert object to string.
     *
     * @param object The object to serialize.
     * @return the result object (e.g., a string).
     * @throws IOException if serialization fails.
     */
    public String serialize(Object object) throws IOException {
        return serialize(object, initialize());
    }

    /**
     * Convert string to java object.
     *
     * @param input input object (e.g., a string).
     * @param type object type.
     * @return deserialization result.
     * @throws IOException if deserialization fails.
     */
    public <T> T deserialize(String input, Class<T> type) throws IOException {
        return deserialize(input, type, initialize());
    }

    /**
     * Deserialize string with custom object mapper.
     *
     * @param input The string to deserialize.
     * @param type The class type.
     * @param objectMapper The custom object mapper.
     * @return The result object.
     * @throws IOException if deserialization fails.
     */
    public <T> T deserialize(String input, Class<T> type, ObjectMapper objectMapper) throws IOException {
        Objects.requireNonNull(input, "input");

        try {
            return objectMapper.readValue(input, type);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            monitor.warning("Failed to deserialize object from JSON-LD");
            throw e;
        }
    }

    /**
     * Serialize object with custom object mapper.
     *
     * @param object The object to serialize.
     * @param objectMapper The custom object mapper.
     * @return The result string.
     * @throws IOException if serialization fails.
     */
    public String serialize(Object object, ObjectMapper objectMapper) throws IOException {
        Objects.requireNonNull(object, "object");

        try {
            var objectNode = objectMapper.convertValue(object, ObjectNode.class);
            return serializeObject(objectNode, objectMapper);
        } catch (IllegalArgumentException e) {
            return serializeString(object, objectMapper);
        }
    }

    /**
     * Registers subtypes to object mapper.
     *
     * @param classes list of classes.
     */
    public void setSubtypes(Class<?>... classes) {
        this.subtypes = classes;
    }

    /**
     * Set context information.
     *
     * @param context context string.
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Return new object mapper.
     *
     * @return the object mapper.
     */
    public ObjectMapper getObjectMapper() {
        return initialize();
    }

    private ObjectMapper initialize() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new XmlGregorianCalendarModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        if (subtypes != null) {
            objectMapper.registerSubtypes(subtypes);
        }

        return objectMapper;
    }

    private String serializeObject(ObjectNode objectNode, ObjectMapper objectMapper) throws JsonProcessingException {
        if (context != null && !context.isEmpty() && !context.isBlank()) {
            objectNode.with("@context");
            objectNode.put("@context", context);
        }

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            monitor.warning("Failed to serialize object to JSON-LD");
            throw e;
        }
    }

    private String serializeString(Object object, ObjectMapper objectMapper) throws JsonProcessingException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            monitor.warning("Failed to serialize string to JSON-LD");
            throw e;
        }
    }
}
