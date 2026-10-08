/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.test;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.CachedDocumentRegistry;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;

import java.util.function.Consumer;

public class TestJsonLd {

    /**
     * Expands a JSON-LD document
     *
     * @param document the document.
     * @return the expanded document.
     */
    public static JsonObject expand(JsonObject document) {
        return expand(document, jsonLd -> {});
    }

    /**
     * Expands a JSON-LD document, with the possibility to customize the JSON-LD service.
     *
     * @param document the document.
     * @param jsonLdCustomization JSON-LD customization function.
     * @return the expanded document.
     */
    public static JsonObject expand(JsonObject document, Consumer<JsonLd> jsonLdCustomization) {
        var jsonLd = new TitaniumJsonLd(new ConsoleMonitor());
        CachedDocumentRegistry.getDocuments().forEach(result -> result
                .onSuccess(c -> jsonLd.registerCachedDocument(c.url(), c.resource()))
        );

        jsonLdCustomization.accept(jsonLd);

        return jsonLd.expand(document).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }

}
