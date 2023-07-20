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

import com.apicatalog.jsonld.loader.DocumentLoader;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.reflection.ReflectionUtil.getFieldValue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class JsonLdExtensionTest {

    @Test
    void createService(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(JsonLdExtension.class);

        var service = extension.createJsonLdService(context);

        assertThat(service).isNotNull();
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry(ServiceExtensionContext context, ObjectFactory factory) throws URISyntaxException {
        Config config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        );
        context = spy(context);
        when(context.getConfig()).thenReturn(config);
        var extension = factory.constructInstance(JsonLdExtension.class);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = getFieldValue("documentLoader", service);
        Map<String, URI> cache = getFieldValue("cache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new URI("file:/tmp/foo/doc.json"));
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry_withSuperfluous(ServiceExtensionContext context, ObjectFactory factory) throws URISyntaxException {
        Config config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.invalid", "should be ignored",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        );
        context = spy(context);
        when(context.getConfig()).thenReturn(config);
        var extension = factory.constructInstance(JsonLdExtension.class);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = getFieldValue("documentLoader", service);
        Map<String, URI> cache = getFieldValue("cache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new URI("file:/tmp/foo/doc.json"));
    }

    @Test
    void verifyCachedDocsFromConfig_multipleValidEntries(ServiceExtensionContext context, ObjectFactory factory) throws URISyntaxException {
        Config config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json",
                "edc.jsonld.document.bar.path", "/tmp/bar/doc.json"
        ));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);
        var extension = factory.constructInstance(JsonLdExtension.class);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = getFieldValue("documentLoader", service);
        Map<String, URI> cache = getFieldValue("cache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new URI("file:/tmp/foo/doc.json"));
        assertThat(cache).containsEntry("http://bar.org/doc.json", new URI("file:/tmp/bar/doc.json"));
    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneIncomplete(ServiceExtensionContext context, ObjectFactory factory) throws URISyntaxException {
        Config config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json"
        ));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);
        var extension = factory.constructInstance(JsonLdExtension.class);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = getFieldValue("documentLoader", service);
        Map<String, URI> cache = getFieldValue("cache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new URI("file:/tmp/foo/doc.json"))
                .noneSatisfy((s, uri) -> assertThat(s).isEqualTo("http://bar.org/doc.json"));

    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneInvalid(ServiceExtensionContext context, ObjectFactory factory) throws URISyntaxException {
        Config config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.invalid", "http://bar.org/doc.json"
        ));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);
        var extension = factory.constructInstance(JsonLdExtension.class);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = getFieldValue("documentLoader", service);
        Map<String, URI> cache = getFieldValue("cache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new URI("file:/tmp/foo/doc.json"))
                .noneSatisfy((s, uri) -> assertThat(s).isEqualTo("http://bar.org/doc.json"));

    }
}
