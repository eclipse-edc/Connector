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
import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.reflection.ReflectionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(DependencyInjectionExtension.class)
class JsonLdExtensionTest {

    @Test
    void createService(TestExtensionContext context, ObjectFactory objectFactory) {
        var extension = objectFactory.constructInstance(JsonLdExtension.class);

        var service = extension.createJsonLdService(context);

        assertThat(service).isNotNull();
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        ));
        var extension = objectFactory.constructInstance(JsonLdExtension.class);

        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        var documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_oneValidEntry_withSuperfluous(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.invalid", "should be ignored",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json")
        ));
        var extension = objectFactory.constructInstance(JsonLdExtension.class);

        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_multipleValidEntries(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json",
                "edc.jsonld.document.bar.path", "/tmp/bar/doc.json"
        )));
        var extension = objectFactory.constructInstance(JsonLdExtension.class);

        var service = (TitaniumJsonLd) extension.createJsonLdService(context);

        DocumentLoader documentLoader = ReflectionUtil.getFieldValue("documentLoader", service);
        Map<String, URI> cache = ReflectionUtil.getFieldValue("uriCache", documentLoader);

        assertThat(cache).containsEntry("http://foo.org/doc.json", new File("/tmp/foo/doc.json").toURI());
        assertThat(cache).containsEntry("http://bar.org/doc.json", new File("/tmp/bar/doc.json").toURI());
    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneIncomplete(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.url", "http://bar.org/doc.json"
        )));

        assertThatThrownBy(() -> objectFactory.constructInstance(JsonLdExtension.class)).isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void verifyCachedDocsFromConfig_multipleEntries_oneInvalid(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.jsonld.document.foo.url", "http://foo.org/doc.json",
                "edc.jsonld.document.foo.path", "/tmp/foo/doc.json",
                "edc.jsonld.document.bar.invalid", "http://bar.org/doc.json"
        )));

        assertThatThrownBy(() -> objectFactory.constructInstance(JsonLdExtension.class)).isInstanceOf(EdcInjectionException.class);
    }
}
