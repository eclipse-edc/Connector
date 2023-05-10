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
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.File;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFileFromResourceName;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class TitaniumJsonLdTest {

    private final int port = getFreePort();
    private final ClientAndServer server = startClientAndServer(port);

    private final Monitor monitor = mock(Monitor.class);

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void expand() {
        var jsonObject = createObjectBuilder()
                .add("test:item", createObjectBuilder()
                        .add(TYPE, "https://some.test.type/schema/")
                        .add("test:key1", "value1")
                        .add("key2", "value2") // will not be contained in the expanded JSON, it lacks the prefix
                        .build())
                .build();

        var expanded = defaultService().expand(jsonObject);

        assertThat(expanded.succeeded()).isTrue();
        assertThat(expanded.getContent().toString())
                .contains("test:item")
                .contains("test:key1")
                .contains("@value\":\"value1\"")
                .doesNotContain("key2")
                .doesNotContain("value2");
    }

    @Test
    void expand_withEmptyArray() {
        var expanded = defaultService().expand(createObjectBuilder().build());

        assertThat(expanded.succeeded()).isTrue();
        assertThat(expanded.getContent()).isEmpty();
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

        var expanded = defaultService().expand(jsonObject);

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

        var compacted = defaultService().compact(expanded);

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

        var service = defaultService();
        service.registerNamespace(prefix, ns);
        var compacted = service.compact(expanded);

        assertThat(compacted.succeeded()).isTrue();
        assertThat(compacted.getContent().getJsonObject(prefix + ":item")).isNotNull();
        assertThat(compacted.getContent().getJsonObject(prefix + ":item").getJsonString(prefix + ":key1").getString()).isEqualTo("value1");
        assertThat(compacted.getContent().getJsonObject(prefix + ":item").getJsonString(prefix + ":key2").getString()).isEqualTo("value2");
    }

    @Test
    void documentResolution_shouldNotCallHttpEndpoint_whenFileContextIsRegistered() {
        var contextUrl = "http://localhost:" + port;
        var jsonObject = createObjectBuilder()
                .add(CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = defaultService();
        service.registerCachedDocument(contextUrl, getFileFromResourceName("test-context.jsonld"));

        var expanded = service.expand(jsonObject);

        server.verifyZeroInteractions();
        assertThat(expanded).isSucceeded().satisfies(json -> {
            assertThat(json.getJsonArray("http://test.org/context/key")).hasSize(1).first()
                    .extracting(JsonValue::asJsonObject)
                    .extracting(it -> it.getString(VALUE))
                    .isEqualTo("value");
        });
    }

    @Test
    void documentResolution_shouldFailByDefault_whenContextIsNotRegisteredAndHttpIsNotEnabled() {
        server.when(request()).respond(response(getResourceFileContentAsString("test-context.jsonld")));
        var contextUrl = "http://localhost:" + port;
        var jsonObject = createObjectBuilder()
                .add(CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = defaultService();
        service.registerCachedDocument("http//any.other/url", new File("any"));

        var expanded = service.expand(jsonObject);

        assertThat(expanded).isFailed();
    }

    @Test
    void documentResolution_shouldCallHttpEndpoint_whenContextIsNotRegistered_andHttpIsEnabled() {
        server.when(request()).respond(response(getResourceFileContentAsString("test-context.jsonld")));
        var contextUrl = "http://localhost:" + port;
        var jsonObject = createObjectBuilder()
                .add(CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = httpEnabledService();
        service.registerCachedDocument("http//any.other/url", new File("any"));

        var expanded = service.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(json -> {
            assertThat(json.getJsonArray("http://test.org/context/key")).hasSize(1).first()
                    .extracting(JsonValue::asJsonObject)
                    .extracting(it -> it.getString(VALUE))
                    .isEqualTo("value");
        });
    }

    private JsonLd httpEnabledService() {
        return new TitaniumJsonLd(monitor, JsonLdConfiguration.Builder.newInstance().httpEnabled(true).build());
    }

    private JsonLd defaultService() {
        return new TitaniumJsonLd(monitor);
    }
}
