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

package org.eclipse.edc.jsonld.document;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResource;

class JarLoaderTest {

    private final JarLoader jarLoader = new JarLoader();

    @Test
    void shouldLoadJsonSchemaFromJar() throws JsonLdError {
        var jarUri = getResource("jar-with-resource-example.jar");
        var resourceInJarUri = URI.create(format("jar:%s!/document/odrl.jsonld", jarUri));

        var document = jarLoader.loadDocument(resourceInJarUri, new DocumentLoaderOptions());

        assertThat(document).isNotNull();
    }

    @Test
    void shouldThrowError_whenSchemeIsNotJar() {
        assertThatThrownBy(() -> jarLoader.loadDocument(URI.create("file://tmp/any"), new DocumentLoaderOptions()))
                .isInstanceOf(JsonLdError.class)
                .hasMessageStartingWith("Unsupported URL scheme");
    }

    @Test
    void shouldThrowErrorWhenJarFileNotFound() {
        var resourceInJarUri = URI.create("jar:file:/tmp/unexistent-file.jar!/document/odrl.jsonld");

        assertThatThrownBy(() -> jarLoader.loadDocument(resourceInJarUri, new DocumentLoaderOptions()))
                .isInstanceOf(JsonLdError.class)
                .hasMessageStartingWith("File not found");
    }

    @Test
    void shouldThrowErrorWhenMalformedUrl() {
        var resourceInJarUri = URI.create("jar:/malformed/url!/document/odrl.jsonld");

        assertThatThrownBy(() -> jarLoader.loadDocument(resourceInJarUri, new DocumentLoaderOptions()))
                .isInstanceOf(JsonLdError.class);
    }

    @Test
    void shouldThrowErrorWhenDocumentFileNotFoundInTheJar() {
        var jarUri = getResource("jar-with-resource-example.jar");
        var resourceInJarUri = URI.create(format("jar:%s!/document/not-existent.jsonld", jarUri));

        assertThatThrownBy(() -> jarLoader.loadDocument(resourceInJarUri, new DocumentLoaderOptions()))
                .isInstanceOf(JsonLdError.class)
                .hasMessageStartingWith("File not found");
    }

}
