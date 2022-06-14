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

package org.eclipse.dataspaceconnector.serializer.jsonld;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.dataspaceconnector.serializer.Serializer;
import org.eclipse.dataspaceconnector.serializer.jsonld.calendar.XmlGregorianCalendarModule;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Service for de-/serializing java objects to/from JSON-LD string using a customized {@link ObjectMapper}.
 */
public class JsonldSerializer implements Serializer<Object, String> {

    private final Monitor monitor;
    private String context;
    private Class<?>[] subtypes;

    /**
     * Constructor.
     *
     * @param monitor monitoring service.
     */
    public JsonldSerializer(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public String serialize(Object object) throws IOException {
        return serialize(object, initialize());
    }

    @Override
    public Object deserialize(String input, Class<?> type) throws IOException {
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
    public Object deserialize(String input, Class<?> type, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(input, type);
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
        String json;
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            monitor.warning("Not able to serialize object to JSON-LD");
            throw e;
        }

        if (context != null && !context.isEmpty() && !context.isBlank()) {
            json = addContextToString(json, context);
        }

        return json;
    }

    private ObjectMapper initialize() {
        var objectMapper = new ObjectMapper();
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

    private String addContextToString(String json, String context) {
        var pos = json.indexOf("{");
        var length = json.length();
        return "{\"@context\": \"" + context + "\", " + json.substring(pos + 1, length);
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
}
