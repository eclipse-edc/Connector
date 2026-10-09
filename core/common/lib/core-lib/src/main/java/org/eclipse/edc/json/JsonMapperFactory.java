/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.json;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsonp.JSONPModule;

/**
 * Factory for the default Jackson {@link JsonMapper} used across the connector.
 * <p>
 * The returned mapper is pre-configured for both plain object (de)serialization and JSON-LD
 * processing, superseding the formerly separate JSON-LD specific mapper.
 */
public class JsonMapperFactory {

    /**
     * Creates the default {@link JsonMapper} with the connector-wide configuration:
     * <ul>
     *     <li>dates are written as ISO-8601 strings rather than numeric timestamps;</li>
     *     <li>unknown and trailing tokens are tolerated during deserialization;</li>
     *     <li>getters are honoured as setters for collection and map properties;</li>
     *     <li>a single value is accepted where an array is expected;</li>
     *     <li>the {@link JSONPModule} is registered to support {@code jakarta.json} types.</li>
     * </ul>
     *
     * @return a new, independently configurable {@link JsonMapper} instance.
     */
    public static JsonMapper defaultJsonMapper() {
        return JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, true)
                .addModule(new JSONPModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
    }
}
