/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JacksonJsonLdTest {


    @Test
    void shouldConfigureSingleValueAsArray() throws JsonProcessingException {
        var mapper = JacksonJsonLd.createObjectMapper();
        assertNotNull(mapper);

        var json = """
                {
                    "list": "value"
                }
                """;

        var test = mapper.readValue(json, TestRecord.class);

        assertThat(test.list).hasSize(1).contains("value");
    }

    private record TestRecord(List<String> list) {
    }
}
