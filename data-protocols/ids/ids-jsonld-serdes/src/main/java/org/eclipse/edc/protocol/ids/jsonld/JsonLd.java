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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.edc.protocol.ids.jsonld.type.calendar.XmlGregorianCalendarModule;

import java.text.SimpleDateFormat;

/**
 * Provides a customized {@link ObjectMapper}.
 */
public final class JsonLd {

    public static ObjectMapper getObjectMapper() {
        var customMapper = new ObjectMapper();
        customMapper.registerModule(new JavaTimeModule());
        customMapper.registerModule(new XmlGregorianCalendarModule());
        customMapper.registerModule(new JsonLdModule());

        customMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        customMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        customMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        customMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        customMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        return customMapper;
    }
}
