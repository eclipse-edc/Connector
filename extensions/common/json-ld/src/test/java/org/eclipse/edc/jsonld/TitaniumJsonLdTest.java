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

import java.net.URI;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFileFromResourceName;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class TitaniumJsonLdTest {

    private final int port = getFreePort();
    private final ClientAndServer server = startClientAndServer(port);
    private final Monitor monitor = mock();

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

        assertThat(expanded).isSucceeded().extracting(Object::toString).asString()
                .contains("test:item")
                .contains("test:key1")
                .contains("@value\":\"value1\"")
                .doesNotContain("key2")
                .doesNotContain("value2");
    }

    @Test
    void expand_shouldFail_whenPropertiesWithoutNamespaceAndContextIsMissing() {
        var emptyJson = createObjectBuilder().add("key", "value").build();

        var expanded = defaultService().expand(emptyJson);

        assertThat(expanded).isFailed();
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

        assertThat(expanded).isSucceeded().extracting(Object::toString).asString()
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

        assertThat(compacted).isSucceeded().satisfies(c -> {
            assertThat(c.getJsonObject(ns + "item")).isNotNull();
            assertThat(c.getJsonObject(ns + "item").getJsonString(ns + "key1").getString()).isEqualTo("value1");
            assertThat(c.getJsonObject(ns + "item").getJsonString(ns + "key2").getString()).isEqualTo("value2");
        });
    }

    @Test
    void compact_withCustomPrefix() {
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

        assertThat(compacted).isSucceeded().satisfies(c -> {
            assertThat(c.getJsonObject(prefix + ":item")).isNotNull();
            assertThat(c.getJsonObject(prefix + ":item").getJsonString(prefix + ":key1").getString()).isEqualTo("value1");
            assertThat(c.getJsonObject(prefix + ":item").getJsonString(prefix + ":key2").getString()).isEqualTo("value2");
        });
    }

    @Test
    void expandAndCompact_withCustomContext() {
        var context = "http://schema.org/";
        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(context).build())
                .add(TYPE, "Person")
                .add("name", "Jane Doe")
                .add("jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(context);
        service.registerCachedDocument(context, getFileFromResourceName("schema-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            assertThat(c.getJsonArray(context + "name").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Jane Doe");
            assertThat(c.getJsonArray(context + "jobTitle").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent());

        assertThat(compacted).isSucceeded().satisfies(c -> {
            assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void expandAndCompact_withCustomContextAndNameClash() {
        var schemaContext = "http://schema.org/";
        var testSchemaContext = "http://test.org/";

        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(schemaContext).add(testSchemaContext).build())
                .add(TYPE, "schema:Person")
                .add("name", "Jane Doe")
                .add("schema:jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(schemaContext);
        service.registerContext(testSchemaContext);
        service.registerCachedDocument(schemaContext, getFileFromResourceName("schema-org-light.jsonld").toURI());
        service.registerCachedDocument(testSchemaContext, getFileFromResourceName("test-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            assertThat(c.getJsonArray(testSchemaContext + "name").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Jane Doe");
            assertThat(c.getJsonArray(schemaContext + "jobTitle").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent());

        assertThat(compacted).isSucceeded().satisfies(c -> {
            assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void expandAndCompact_withCustomContextAndCustomScope() {
        var schemaContext = "http://schema.org/";
        var testSchemaContext = "http://test.org/";
        var customScope = "customScope";

        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(schemaContext).add(testSchemaContext).build())
                .add(TYPE, "schema:Person")
                .add("name", "Jane Doe")
                .add("schema:jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(schemaContext);
        service.registerContext(testSchemaContext, customScope);
        service.registerCachedDocument(schemaContext, getFileFromResourceName("schema-org-light.jsonld").toURI());
        service.registerCachedDocument(testSchemaContext, getFileFromResourceName("test-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            assertThat(c.getJsonArray(testSchemaContext + "name").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Jane Doe");
            assertThat(c.getJsonArray(schemaContext + "jobTitle").get(0).asJsonObject().getJsonString(VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent(), customScope);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void documentResolution_shouldNotCallHttpEndpoint_whenFileContextIsRegistered() {
        var contextUrl = "http://localhost:" + port;
        var jsonObject = createObjectBuilder()
                .add(CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = defaultService();
        service.registerCachedDocument(contextUrl, getFileFromResourceName("test-context.jsonld").toURI());

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
        service.registerCachedDocument("http//any.other/url", URI.create("http://localhost:" + server.getLocalPort()));

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
