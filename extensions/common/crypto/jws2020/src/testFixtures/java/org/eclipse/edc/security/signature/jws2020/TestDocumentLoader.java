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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;

import java.io.IOException;
import java.net.URI;

/**
 * JSON-LD document loader that allows to "redirect" the loading of remote documents (contexts,...).
 * For example, referencing a remote context, or a remote verificationMethod would fail, if that document doesn't exist, but we need it
 * for testing, so we can "redirect" the pointer to the local test resources folder.
 */
public class TestDocumentLoader implements DocumentLoader {
    private final String base;
    private final DocumentLoader baseLoader;
    private final String resourcePath;

    public TestDocumentLoader(String base, String resourcePath, DocumentLoader baseLoader) {
        this.base = base;
        this.resourcePath = resourcePath;
        this.baseLoader = baseLoader;
    }

    @Override
    public Document loadDocument(URI uri, DocumentLoaderOptions options) throws JsonLdError {
        Document document;
        var url = uri.toString();
        if (url.startsWith(base)) {
            try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(rewrite(uri))) {
                document = JsonDocument.of(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            document = baseLoader.loadDocument(uri, options);
        }
        return document;
    }

    private String rewrite(URI url) {
        var path = resourcePath + url.toString().replace(base, "");
        if (!path.endsWith(".json")) {
            path += ".json";
        }
        return path;
    }
}
