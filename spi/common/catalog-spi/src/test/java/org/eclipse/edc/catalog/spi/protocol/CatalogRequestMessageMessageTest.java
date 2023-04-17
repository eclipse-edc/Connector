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

package org.eclipse.edc.catalog.spi.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogRequestMessageMessageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialize() throws JsonProcessingException {
        var message = CatalogRequestMessage.Builder.newInstance().build();

        var json = mapper.writeValueAsString(message);

        assertThat(json)
                .isNotNull()
                .contains("\"filter\":null");
    }

    @Test
    void serialize_withFilter() throws JsonProcessingException {
        var message = CatalogRequestMessage.Builder.newInstance()
                .filter(QuerySpec.none())
                .build();

        var json = mapper.writeValueAsString(message);

        assertThat(json)
                .isNotNull()
                .doesNotContain("\"filter\":null")
                .contains("\"filter\":{");
    }

    @Test
    void deserialize() throws JsonProcessingException {
        var json = "{" +
                "\"filter\": null" +
                "}";

        var message = mapper.readValue(json, CatalogRequestMessage.class);

        assertThat(message)
                .isNotNull()
                .matches(m -> m.getFilter() == null);
    }

    @Test
    void deserialize_withFilter() throws JsonProcessingException {
        var json = "{" +
                "\"filter\": {}" +
                "}";

        var message = mapper.readValue(json, CatalogRequestMessage.class);

        assertThat(message)
                .isNotNull()
                .matches(m -> m.getFilter() != null);
    }
}
