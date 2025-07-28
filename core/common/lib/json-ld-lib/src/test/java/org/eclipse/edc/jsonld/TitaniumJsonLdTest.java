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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class TitaniumJsonLdTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();


    private final Monitor monitor = mock();


    @Test
    void expand() {
        var jsonObject = createObjectBuilder()
                .add("test:item", createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, "https://some.test.type/schema/")
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
    void expand_shouldSucceed_whenChecksDisabledOnMissingContext() {
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, createObjectBuilder().build())
                .add("custom:item", "foo")
                .build();
        var jsonLd = defaultService(JsonLdConfiguration.Builder.newInstance().checkPrefixes(false).build());

        jsonLd.registerNamespace("custom", "https://custom.namespace.org/schema/");

        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded();
    }

    @Test
    void expand_shouldFail_whenMissingContext() {
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, createObjectBuilder().build())
                .add("custom:item", "foo")
                .build();
        var jsonLd = defaultService();

        jsonLd.registerNamespace("custom", "https://custom.namespace.org/schema/");

        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isFailed();
    }

    @Test
    void expand_withCustomContext() {
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, createObjectBuilder().add("custom", "https://custom.namespace.org/schema/").build())
                .add("test:item", createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, "https://some.test.type/schema/")
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
                        .add(JsonLdKeywords.TYPE, ns + "TestItem")
                        .add(ns + "key1", createArrayBuilder().add(createObjectBuilder().add(JsonLdKeywords.VALUE, "value1").build()).build())
                        .add(ns + "key2", createArrayBuilder().add(createObjectBuilder().add(JsonLdKeywords.VALUE, "value2").build()).build()))
                .build();

        var compacted = defaultService().compact(expanded);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getJsonObject(ns + "item")).isNotNull();
            Assertions.assertThat(c.getJsonObject(ns + "item").getJsonString(ns + "key1").getString()).isEqualTo("value1");
            Assertions.assertThat(c.getJsonObject(ns + "item").getJsonString(ns + "key2").getString()).isEqualTo("value2");
        });
    }

    @Test
    void compact_withVocab() {
        var expanded = Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "item", "test")
                .build();
        var jsonLd = defaultService();
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        var compacted = jsonLd.compact(expanded);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getString("item")).isEqualTo("test");
        });
    }

    @Test
    void compact_withVocabDisabled() {
        var expanded = Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "item", "test")
                .build();
        var jsonLd = defaultService(JsonLdConfiguration.Builder.newInstance().avoidVocab(true).build());
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        var compacted = jsonLd.compact(expanded);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getString(EDC_NAMESPACE + "item")).isEqualTo("test");
        });
    }

    @Test
    void compact_withCustomPrefix() {
        var ns = "https://test.org/schema/";
        var prefix = "customContext";
        var expanded = createObjectBuilder()
                .add(ns + "item", createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, ns + "TestItem")
                        .add(ns + "key1", createArrayBuilder().add(createObjectBuilder().add(JsonLdKeywords.VALUE, "value1").build()).build())
                        .add(ns + "key2", createArrayBuilder().add(createObjectBuilder().add(JsonLdKeywords.VALUE, "value2").build()).build()))
                .build();

        var service = defaultService();
        service.registerNamespace(prefix, ns);
        var compacted = service.compact(expanded);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getJsonObject(prefix + ":item")).isNotNull();
            Assertions.assertThat(c.getJsonObject(prefix + ":item").getJsonString(prefix + ":key1").getString()).isEqualTo("value1");
            Assertions.assertThat(c.getJsonObject(prefix + ":item").getJsonString(prefix + ":key2").getString()).isEqualTo("value2");
        });
    }

    @Test
    void expandAndCompact_withCustomContext() {
        var context = "http://schema.org/";
        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(context).build())
                .add(JsonLdKeywords.TYPE, "Person")
                .add("name", "Jane Doe")
                .add("jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(context);
        service.registerCachedDocument(context, TestUtils.getFileFromResourceName("schema-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getJsonArray(context + "name").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Jane Doe");
            Assertions.assertThat(c.getJsonArray(context + "jobTitle").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent());

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void expandAndCompact_withCustomContextAndNameClash() {
        var schemaContext = "http://schema.org/";
        var testSchemaContext = "http://test.org/";

        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(schemaContext).add(testSchemaContext).build())
                .add(JsonLdKeywords.TYPE, "schema:Person")
                .add("name", "Jane Doe")
                .add("schema:jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(schemaContext);
        service.registerContext(testSchemaContext);
        service.registerCachedDocument(schemaContext, TestUtils.getFileFromResourceName("schema-org-light.jsonld").toURI());
        service.registerCachedDocument(testSchemaContext, TestUtils.getFileFromResourceName("test-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getJsonArray(testSchemaContext + "name").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Jane Doe");
            Assertions.assertThat(c.getJsonArray(schemaContext + "jobTitle").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent());

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void expandAndCompact_withCustomContextAndCustomScope() {
        var schemaContext = "http://schema.org/";
        var testSchemaContext = "http://test.org/";
        var customScope = "customScope";

        var input = createObjectBuilder()
                .add("@context", createArrayBuilder().add(schemaContext).add(testSchemaContext).build())
                .add(JsonLdKeywords.TYPE, "schema:Person")
                .add("name", "Jane Doe")
                .add("schema:jobTitle", "Professor")
                .build();

        var service = defaultService();
        service.registerContext(schemaContext);
        service.registerContext(testSchemaContext, customScope);
        service.registerCachedDocument(schemaContext, TestUtils.getFileFromResourceName("schema-org-light.jsonld").toURI());
        service.registerCachedDocument(testSchemaContext, TestUtils.getFileFromResourceName("test-org-light.jsonld").toURI());

        var expanded = service.expand(input);

        assertThat(expanded).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c.getJsonArray(testSchemaContext + "name").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Jane Doe");
            Assertions.assertThat(c.getJsonArray(schemaContext + "jobTitle").get(0).asJsonObject().getJsonString(JsonLdKeywords.VALUE).getString()).isEqualTo("Professor");
        });

        var compacted = service.compact(expanded.getContent(), customScope);

        assertThat(compacted).isSucceeded().satisfies(c -> {
            Assertions.assertThat(c).isEqualTo(input);
        });
    }

    @Test
    void documentResolution_shouldNotCallHttpEndpoint_whenFileContextIsRegistered() {
        var contextUrl = "http://localhost:" + server.getPort();
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = defaultService();
        service.registerCachedDocument(contextUrl, TestUtils.getFileFromResourceName("test-context.jsonld").toURI());

        var expanded = service.expand(jsonObject);

        server.verify(0, getRequestedFor(anyUrl()));
        assertThat(expanded).isSucceeded().satisfies(json -> {
            Assertions.assertThat(json.getJsonArray("http://test.org/context/key")).hasSize(1).first()
                    .extracting(JsonValue::asJsonObject)
                    .extracting(it -> it.getString(JsonLdKeywords.VALUE))
                    .isEqualTo("value");
        });
    }

    @Test
    void documentResolution_shouldFailByDefault_whenContextIsNotRegisteredAndHttpIsNotEnabled() {
        server.stubFor(get(anyUrl()).willReturn(ok(TestUtils.getResourceFileContentAsString("test-context.jsonld"))));
        var contextUrl = "http://localhost:" + server.getPort();
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = defaultService();

        var expanded = service.expand(jsonObject);

        assertThat(expanded).isFailed();
    }

    @Test
    void documentResolution_shouldCallHttpEndpoint_whenContextIsNotRegistered_andHttpIsEnabled() {
        server.stubFor(get(anyUrl()).willReturn(ok(TestUtils.getResourceFileContentAsString("test-context.jsonld"))));

        var contextUrl = "http://localhost:" + server.getPort();
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = httpEnabledService();
        service.registerCachedDocument("http//any.other/url", URI.create("http://localhost:" + server.getPort()));

        var expanded = service.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(json -> {
            Assertions.assertThat(json.getJsonArray("http://test.org/context/key")).hasSize(1).first()
                    .extracting(JsonValue::asJsonObject)
                    .extracting(it -> it.getString(JsonLdKeywords.VALUE))
                    .isEqualTo("value");
        });

    }


    @Test
    void documentResolution_shouldCallHttpEndpointOnlyOnce_whenContextIsNotRegistered_andHttpIsEnabled() {
        server.stubFor(get(anyUrl()).willReturn(ok(TestUtils.getResourceFileContentAsString("test-context.jsonld"))));
        var contextUrl = "http://localhost:" + server.getPort();
        var jsonObject = createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, contextUrl)
                .add("test:key", "value")
                .build();
        var service = httpEnabledService();
        service.registerCachedDocument("http//any.other/url", URI.create("http://localhost:" + server.getPort()));

        assertThat(service.expand(jsonObject)).isSucceeded();
        assertThat(service.expand(jsonObject)).isSucceeded();

        server.verify(1, getRequestedFor(anyUrl()));

    }

    private JsonLd httpEnabledService() {
        return new TitaniumJsonLd(monitor, JsonLdConfiguration.Builder.newInstance().httpEnabled(true).build());
    }

    private JsonLd defaultService() {
        return defaultService(JsonLdConfiguration.Builder.newInstance().build());
    }

    private JsonLd defaultService(JsonLdConfiguration configuration) {
        return new TitaniumJsonLd(monitor, configuration);
    }
}
