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
import org.eclipse.dataspaceconnector.serializer.jsonld.calendar.XmlGregorianCalendarModule;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.text.SimpleDateFormat;

public class JsonldSerializer {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private String context;

    /**
     * Constructor.
     *
     * @param monitor monitoring service.
     */
    public JsonldSerializer(Monitor monitor) {
        this.monitor = monitor;

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.registerModule(new XmlGregorianCalendarModule());
    }

    /**
     * Registers subtypes to object mapper.
     *
     * @param classes list of classes.
     */
    public void registerSubtypes(Class<?>... classes) {
        objectMapper.registerSubtypes(classes);
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Converts string to JSON-LD.
     *
     * @param obj object that should be converted.
     * @return string as JSON-LD.
     * @throws JsonProcessingException if string could not be serialized.
     */
    public String toRdf(Object obj) throws JsonProcessingException {
        String json;
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            monitor.warning("Not able to serialize object to JSON-LD");
            throw e;
        }

        if (context != null && !context.isEmpty() && !context.isBlank()) {
            json = addContextToString(json, context);
        }

        return json;
    }

    private String addContextToString(String json, String context) {
        var pos = json.indexOf("{");
        var length = json.length();
        return "{\"@context\": \"" + context + "\", " + json.substring(pos + 1, length);
    }

    /**
     * Returns object mapper.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
