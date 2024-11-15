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
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.reflection.ReflectionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class JsonLdExtensionTest {

    @Test
    void createService(ServiceExtensionContext context, JsonLdExtension extension) {
        var service = extension.createJsonLdService(context);

        assertThat(service).isNotNull();
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry(ServiceExtensionContext context, JsonLdExtension extension) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        );
        when(context.getConfig()).thenReturn(config);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        var documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry_withSuperfluous(ServiceExtensionContext context, JsonLdExtension extension) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.invalid", "should be ignored",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        );
        when(context.getConfig()).thenReturn(config);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_multipleValidEntries(ServiceExtensionContext context, JsonLdExtension extension) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json",
                "edc.jsonld.document.bar.path", "/tmp/bar/doc.json"
        ));
        when(context.getConfig()).thenReturn(config);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
        assertThat(cache).containsEntry("http://bar.org/doc.json", new File("/tmp/bar/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneIncomplete(ServiceExtensionContext context, JsonLdExtension extension) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json"
        ));
        when(context.getConfig()).thenReturn(config);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI())
                .noneSatisfy((s, uri) -> assertThat(s).isEqualTo("http://bar.org/doc.json"));

    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneInvalid(ServiceExtensionContext context, JsonLdExtension extension) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.invalid", "http://bar.org/doc.json"
        ));
        when(context.getConfig()).thenReturn(config);
        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI())
                .noneSatisfy((s, uri) -> assertThat(s).isEqualTo("http://bar.org/doc.json"));

    }
}
