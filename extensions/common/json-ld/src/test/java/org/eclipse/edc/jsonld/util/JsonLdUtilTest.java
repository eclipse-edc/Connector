/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.jsonld.util;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;

class JsonLdUtilTest {
    
    private static final String PREFIX = "prefix";
    private static final String SCHEMA = "http://schema/";
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    
    @Test
    void expand() {
        var json = jsonFactory.createObjectBuilder()
                .add(CONTEXT, jsonFactory.createObjectBuilder().add(PREFIX, SCHEMA).build())
                .add(PREFIX + ":foo", Json.createValue("value"))
                .build();
        
        var resultArray = JsonLdUtil.expand(json);
        
        assertThat(resultArray)
                .isNotNull()
                .hasSize(1);
        
        var result = (JsonObject) resultArray.get(0);
        assertThat(result.get(CONTEXT)).isNull();
        assertThat(result.get(PREFIX + ":foo")).isNull();
        assertThat(result.get(SCHEMA + "foo")).isNotNull();
    }
    
    @Test
    void compact() {
        var json = jsonFactory.createObjectBuilder()
                .add(SCHEMA + "foo", Json.createValue("value"))
                .build();
        var context = jsonFactory.createObjectBuilder()
                .add(PREFIX, SCHEMA)
                .build();
        
        var result = JsonLdUtil.compact(json, context);
        
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(CONTEXT)).isNotNull();
        assertThat(result.get(PREFIX + ":foo")).isNotNull();
    }
}
