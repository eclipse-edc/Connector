/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld;

import jakarta.json.Json;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;

class TitaniumJsonLdTest {

    private final TitaniumJsonLd service = new TitaniumJsonLd(mock(Monitor.class));

    @Test
    void expand() {
        var jsonObject = createObjectBuilder()
                .add("test:item", createObjectBuilder()
                        .add(TYPE, "https://some.test.type/schema/")
                        .add("test:key1", "value1")
                        .add("key2", "value2") // will not be contained in the expanded JSON, it lacks the prefix
                        .build())
                .build();

        var expanded = service.expand(jsonObject);
        assertThat(expanded.succeeded()).isTrue();
        assertThat(expanded.getContent().toString())
                .contains("test:item")
                .contains("test:key1")
                .contains("@value\":\"value1\"")
                .doesNotContain("key2")
                .doesNotContain("value2");
    }

    @Test
    void expand_withCustomContext() {
        var jsonObject = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add("custom", "https://custom.namespace.org/schema/").build())
                .add("test:item", createObjectBuilder()
                        .add(TYPE, "https://some.test.type/schema/")
                        .add("test:key1", "value1")
                        .add("custom:key2", "value2") // will not be contained in the expanded JSON, it lacks the prefix
                        .build())
                .build();

        var expanded = service.expand(jsonObject);
        assertThat(expanded.succeeded()).isTrue();
        assertThat(expanded.getContent().toString())
                .contains("test:item")
                .contains("test:key1")
                .contains("@value\":\"value1\"")
                .contains("https://custom.namespace.org/schema/key2")
                .contains("@value\":\"value2\"");
    }

    @Test
    void compact() {
        var ns = "https://test.org/schema/";
        var expanded = Json.createObjectBuilder()
                .add(ns + "item", createObjectBuilder()
                        .add(TYPE, ns + "TestItem")
                        .add(ns + "key1", createArrayBuilder().add(createObjectBuilder().add(VALUE, "value1").build()).build())
                        .add(ns + "key2", createArrayBuilder().add(createObjectBuilder().add(VALUE, "value2").build()).build()))
                .build();

        var compacted = service.compact(expanded);

        assertThat(compacted.succeeded()).isTrue();
        assertThat(compacted.getContent().getJsonObject(ns + "item")).isNotNull();
        assertThat(compacted.getContent().getJsonObject(ns + "item").getJsonString(ns + "key1").getString()).isEqualTo("value1");
        assertThat(compacted.getContent().getJsonObject(ns + "item").getJsonString(ns + "key2").getString()).isEqualTo("value2");
    }

    @Test
    void compact_withCustomContext() {
        var ns = "https://test.org/schema/";
        var prefix = "customContext";
        var expanded = createObjectBuilder()
                .add(ns + "item", createObjectBuilder()
                        .add(TYPE, ns + "TestItem")
                        .add(ns + "key1", createArrayBuilder().add(createObjectBuilder().add(VALUE, "value1").build()).build())
                        .add(ns + "key2", createArrayBuilder().add(createObjectBuilder().add(VALUE, "value2").build()).build()))
                .build();

        service.registerNamespace(prefix, ns);
        var compacted = service.compact(expanded);

        assertThat(compacted.succeeded()).isTrue();
        assertThat(compacted.getContent().getJsonObject(prefix + ":item")).isNotNull();
        assertThat(compacted.getContent().getJsonObject(prefix + ":item").getJsonString(prefix + ":key1").getString()).isEqualTo("value1");
        assertThat(compacted.getContent().getJsonObject(prefix + ":item").getJsonString(prefix + ":key2").getString()).isEqualTo("value2");
    }
}